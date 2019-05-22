/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.dashboard.listener.sofa;

import com.alipay.sofa.dashboard.cache.RegistryDataCache;
import com.alipay.sofa.dashboard.constants.SofaDashboardConstants;
import com.alipay.sofa.dashboard.domain.RpcConsumer;
import com.alipay.sofa.dashboard.domain.RpcProvider;
import com.alipay.sofa.dashboard.domain.RpcService;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/5/5 8:02 PM
 * @since:
 **/
@Component
public class SofaRegistryRestClient {

    private static final Logger LOGGER                          = LoggerFactory
                                                                    .getLogger(SofaRegistryRestClient.class);

    private static final String DATA_PREFIX                     = "digest";
    private static final String REGISTRY_QUERY_SUB_SESSION_DATA = DATA_PREFIX + "/sub/data/query";
    private static final String REGISTRY_QUERY_PUB_SESSION_DATA = DATA_PREFIX + "/pub/data/query";
    private static final String REGISTRY_QUERY_DATA_INFO_IDS    = DATA_PREFIX
                                                                  + "/getDataInfoIdList";
    private static final String REGISTRY_QUERY_CHECK_SUM        = DATA_PREFIX
                                                                  + "/checkSumDataInfoIdList";

    @Autowired
    private RegistryDataCache   registryDataCache;

    @Autowired
    private RestTemplate        restTemplate;

    private static String       sessionAddress;

    private static int          port;

    public List<String> syncAllDataInfoIds() {
        List<String> dataIds = new ArrayList<>();
        String httpUrl = buildRequestUrl(REGISTRY_QUERY_DATA_INFO_IDS);
        try {
            ResponseEntity<List> forEntity = restTemplate.getForEntity(httpUrl, List.class);
            dataIds = forEntity.getBody();
            if (dataIds == null) {
                dataIds = new ArrayList<>();
            }
        } catch (Throwable t) {
            LOGGER.error(
                "Failed to sync all dataInfoIds from session. query url [" + httpUrl + "]", t);
        }
        return dataIds;
    }

    public void refreshAllSessionDataByDataInfoIds(List<String> dataIds) {
        // 先清理当前缓存中所有的 dataId
        Map<String, RpcService> serviceMap = registryDataCache.fetchService();
        if (!serviceMap.isEmpty()) {
            registryDataCache.removeService(new ArrayList<>(serviceMap.values()));
        }
        // 重新载入 dataIds
        addRpcService(dataIds);
        // 获取 sub 和 pub 数据
        dataIds.forEach((dataInfoId) -> {
            String dataId = extractDataId(dataInfoId);
            if (StringUtils.isNotBlank(dataId)){
                registryDataCache.removeConsumers(dataId, registryDataCache.fetchConsumersByService(dataId));
                registryDataCache.removeProviders(dataId, registryDataCache.fetchProvidersByService(dataId));
                getSessionDataByDataInfoId(dataInfoId);
            }
        });
    }

    private String extractDataId(String dataInfoId) {
        if (StringUtils.isBlank(dataInfoId)) {
            return SofaDashboardConstants.EMPTY;
        }
        if (dataInfoId.contains(SofaDashboardConstants.DATA_ID_SEPARATOR)) {
            String[] split = dataInfoId.split(SofaDashboardConstants.DATA_ID_SEPARATOR);
            return split[0];
        }
        return SofaDashboardConstants.EMPTY;
    }

    public Integer checkSum() {
        String pubUrl = buildRequestUrl(REGISTRY_QUERY_CHECK_SUM);
        ResponseEntity<Integer> checkSumResp = restTemplate.getForEntity(pubUrl, Integer.class);
        return checkSumResp.getBody();
    }

    private void addRpcService(List<String> dataIds) {
        List<RpcService> serviceList = new ArrayList<>();
        dataIds.forEach((dataInfoId) -> {
            RpcService service = new RpcService();
            service.setServiceName(dataInfoId.split(SofaDashboardConstants.DATA_ID_SEPARATOR)[0]);
            serviceList.add(service);
        });
        registryDataCache.addService(serviceList);
    }

    public void getSessionDataByDataInfoId(String dataInfoId) {
        querySubscribers(dataInfoId);
        queryPublishers(dataInfoId);
    }

    public void querySubscribers(String dataInfoId) {
        String subUrl = buildRequestUrl(REGISTRY_QUERY_SUB_SESSION_DATA);
        subUrl += "?dataInfoId={1}";
        ResponseEntity<Map> subResponse = restTemplate.getForEntity(subUrl, Map.class, dataInfoId);
        if (subResponse != null && subResponse.getBody() != null) {
            Map<String, List<Map>> subMap = subResponse.getBody();
            Set<String> subKeys = subMap.keySet();
            subKeys.forEach((key) -> {
                List<Map> subscriberList = subMap.get(key);
                List<RpcConsumer> consumers = new ArrayList<>();
                for (Map subscriberMap : subscriberList) {
                    RpcConsumer consumer = convertRpcConsumerFromMap(subscriberMap);
                    consumers.add(consumer);
                }
                if (!consumers.isEmpty()) {
                    registryDataCache.addConsumers(consumers.get(0).getServiceName(), consumers);
                }
            });
        }
    }

    public void queryPublishers(String dataInfoId) {
        String pubUrl = buildRequestUrl(REGISTRY_QUERY_PUB_SESSION_DATA);
        pubUrl += "?dataInfoId={1}";
        ResponseEntity<Map> pubResponse = restTemplate.getForEntity(pubUrl, Map.class, dataInfoId);
        if (pubResponse != null && pubResponse.getBody() != null) {
            Map<String, List<Map>> subMap = pubResponse.getBody();
            Set<String> subKeys = subMap.keySet();
            subKeys.forEach((key) -> {
                List<Map> publisherList = subMap.get(key);
                List<RpcProvider> providers = new ArrayList<>();
                for (Map publisherMap : publisherList) {
                    RpcProvider provider = convertRpcProviderFromMap(publisherMap);
                    providers.add(provider);
                }
                if (!providers.isEmpty()) {
                    registryDataCache.addProviders(providers.get(0).getServiceName(), providers);
                }
            });
        }
    }

    private RpcProvider convertRpcProviderFromMap(Map publisherMap) {
        RpcProvider provider = new RpcProvider();
        provider.setAppName(getEmptyStringIfNull(publisherMap, SofaDashboardConstants.APP_NAME));
        provider.setServiceName(getEmptyStringIfNull(publisherMap,
            SofaDashboardConstants.REGISTRY_DATA_ID_KEY));
        String processId = getEmptyStringIfNull(publisherMap,
            SofaDashboardConstants.REGISTRY_PROCESS_ID_KEY);
        if (processId.contains(SofaDashboardConstants.COLON)) {
            provider.setAddress(processId.split(SofaDashboardConstants.COLON)[0]);
            provider.setPort(Integer.valueOf(processId.split(SofaDashboardConstants.COLON)[1]));
        } else {
            Object sourceAddress = publisherMap
                .get(SofaDashboardConstants.REGISTRY_SOURCE_ADDRESS_KEY);
            if (sourceAddress instanceof Map) {
                String ipAddress = getEmptyStringIfNull((Map) sourceAddress,
                    SofaDashboardConstants.REGISTRY_IP_KEY);
                String port = getEmptyStringIfNull((Map) sourceAddress, SofaDashboardConstants.PORT);
                provider.setAddress(ipAddress);
                provider.setPort(Integer.valueOf(StringUtils.isBlank(port) ? "0" : port));
            }
        }
        Map<String, String> attributes = (Map<String, String>) publisherMap
            .get(SofaDashboardConstants.REGISTRY_ATTRIBUTES);
        provider.setParameters(attributes);
        return provider;
    }

    private RpcConsumer convertRpcConsumerFromMap(Map subscriberMap) {
        RpcConsumer consumer = new RpcConsumer();
        consumer.setAppName(getEmptyStringIfNull(subscriberMap, SofaDashboardConstants.APP_NAME));
        consumer.setServiceName(getEmptyStringIfNull(subscriberMap,
            SofaDashboardConstants.REGISTRY_DATA_ID_KEY));
        String processId = getEmptyStringIfNull(subscriberMap,
            SofaDashboardConstants.REGISTRY_PROCESS_ID_KEY);
        if (processId.contains(SofaDashboardConstants.COLON)) {
            consumer.setAddress(processId.split(SofaDashboardConstants.COLON)[0]);
            consumer.setPort(Integer.valueOf(processId.split(SofaDashboardConstants.COLON)[1]));
        } else {
            Object sourceAddress = subscriberMap
                .get(SofaDashboardConstants.REGISTRY_SOURCE_ADDRESS_KEY);
            if (sourceAddress instanceof Map) {
                String ipAddress = getEmptyStringIfNull((Map) sourceAddress,
                    SofaDashboardConstants.REGISTRY_IP_KEY);
                String port = getEmptyStringIfNull((Map) sourceAddress, SofaDashboardConstants.PORT);
                consumer.setAddress(ipAddress);
                consumer.setPort(Integer.valueOf(StringUtils.isBlank(port) ? "0" : port));
            }
        }
        Map<String, String> attributes = (Map<String, String>) subscriberMap
            .get(SofaDashboardConstants.REGISTRY_ATTRIBUTES);
        consumer.setParameters(attributes);
        return consumer;
    }

    /**
     * build query request url
     *
     * @param resource
     * @return
     */
    private String buildRequestUrl(String resource) {
        StringBuilder sb = new StringBuilder();
        sb.append(SofaDashboardConstants.HTTP_SCHEME).append(sessionAddress)
            .append(SofaDashboardConstants.COLON).append(port);
        sb.append(SofaDashboardConstants.SEPARATOR).append(resource);
        return sb.toString();
    }

    /**
     * Extract value from map ,if null return empty String
     *
     * @param map
     * @param key
     * @return
     */
    private String getEmptyStringIfNull(Map map, String key) {
        if (map == null || map.size() <= 0) {
            return StringUtils.EMPTY;
        }
        Object valueObject = map.get(key);
        String valueStr;
        try {
            valueStr = (String) valueObject;
        } catch (Throwable throwable) {
            return StringUtils.EMPTY;
        }
        return StringUtils.isBlank(valueStr) ? StringUtils.EMPTY : valueStr;
    }

    public void init(RegistryConfig registryConfig) {
        String endPointAddress = registryConfig.getAddress();
        if (!endPointAddress.contains(SofaDashboardConstants.COLON)) {
            throw new RuntimeException(
                "Please check your session address.Illegal session address is [" + endPointAddress
                        + "]");
        }
        sessionAddress = endPointAddress.split(SofaDashboardConstants.COLON)[0];
        port = Integer.valueOf(endPointAddress.split(SofaDashboardConstants.COLON)[1]);
    }
}
