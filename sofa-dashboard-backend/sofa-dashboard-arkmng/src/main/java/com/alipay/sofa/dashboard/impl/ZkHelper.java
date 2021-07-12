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
package com.alipay.sofa.dashboard.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.sofa.ark.api.ResponseCode;
import com.alipay.sofa.ark.spi.model.BizState;
import com.alipay.sofa.dashboard.constants.SofaDashboardConstants;
import com.alipay.sofa.dashboard.model.AppUnitModel;
import com.alipay.sofa.dashboard.model.BizModel;
import com.alipay.sofa.dashboard.model.ClientResponseModel;
import com.alipay.sofa.dashboard.utils.FastJsonUtils;
import com.alipay.sofa.dashboard.zookeeper.ZkCommandClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/2/14 5:24 PM
 * @since:
 **/
@Component
public class ZkHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkHelper.class);

    @Autowired
    ZkCommandClient             zkCommandClient;

    /**
     * 根据应用名获取当前应用的所有实例
     *
     * @param appName
     * @return
     * @throws Exception
     */
    public List<AppUnitModel> getArkAppFromZookeeper(String appName, String pluginName,
                                                     String version) throws Exception {
        List<AppUnitModel> applications = new ArrayList<>();
        List<String> ipList = getInstanceIpList(appName);
        if (ipList.size() == 0) {
            return applications;
        }
        for (String ip : ipList) {
            AppUnitModel application = new AppUnitModel();
            application.setIp(ip);
            // todo 如何获取ark-biz 状态数据 ？？？不在依赖 actuator 去主动获取
            application.setStatus(getAppState(appName, ip, pluginName, version));
            applications.add(application);
        }
        return applications;
    }

    /**
     * 获取 /sofa-ark/appName 下面所有的实例 IP
     * @param appName
     * @return
     */
    private List<String> getInstanceIpList(String appName) {
        List<String> result = null;
        CuratorFramework curatorClient = zkCommandClient.getCuratorClient();
        String arkAppBasePath = SofaDashboardConstants.SOFA_ARK_ROOT
                                + SofaDashboardConstants.SEPARATOR + appName;
        try {
            if (checkExist(arkAppBasePath, curatorClient)) {
                // 根据应用名获取所有实例信息
                result = curatorClient.getChildren().forPath(arkAppBasePath);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get ark app instance ip list.", e);
        } finally {
            if (result == null) {
                result = new ArrayList<>();
            }
        }
        return result;
    }

    /**
     * 获取 /sofa-ark 下面所有的应用
     * @return
     */
    public List<String> getArkAppList() {
        List<String> result = null;
        CuratorFramework curatorClient = zkCommandClient.getCuratorClient();
        String arkAppBasePath = SofaDashboardConstants.SOFA_ARK_ROOT;
        try {
            if (checkExist(arkAppBasePath, curatorClient)) {
                // 获取所有应用信息
                result = curatorClient.getChildren().forPath(arkAppBasePath);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get ark app list.", e);
        } finally {
            if (result == null) {
                result = new ArrayList<>();
            }
        }
        return result;
    }

    /**
     * 获取当前应用实例的个数
     *
     * @return
     */
    public int getArkAppCount(String appName) throws Exception {
        return getInstanceIpList(appName).size();
    }

    private boolean checkExist(String path, CuratorFramework curatorClient) {
        boolean isExist = false;
        if (!StringUtils.isEmpty(path) && curatorClient != null) {
            try {
                Stat stat = curatorClient.checkExists().forPath(path);
                if (stat != null) {
                    isExist = true;
                }
            } catch (Exception e) {
                isExist = false;
            }
        }
        return isExist;
    }

    /**
     * 从 ZK 获取   apps/biz/
     * @param appName
     * @param ip
     * @param pluginName
     * @param version
     * @return
     */
    public String getAppState(String appName, String ip, String pluginName, String version)
                                                                                           throws Exception {
        String bizPath = SofaDashboardConstants.SOFA_BOOT_CLIENT_ROOT
                         + SofaDashboardConstants.SOFA_BOOT_CLIENT_BIZ;
        CuratorFramework curatorClient = zkCommandClient.getCuratorClient();
        if (curatorClient.checkExists().forPath(bizPath) != null) {
            String bizAppPath = bizPath + SofaDashboardConstants.SEPARATOR + appName
                                + SofaDashboardConstants.SEPARATOR + ip;
            if (curatorClient.checkExists().forPath(bizAppPath) != null) {
                byte[] bytes = curatorClient.getData().forPath(bizAppPath);
                String data = new String(bytes);
                JSONObject json = JSON.parseObject(data);
                String bizInfos = FastJsonUtils.getString(json, "bizInfos");
                JSONArray array = JSONArray.parseArray(bizInfos);
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    String bizName = FastJsonUtils.getString(item, "bizName");
                    String bizVersion = FastJsonUtils.getString(item, "bizVersion");
                    if (bizName.equalsIgnoreCase(pluginName)
                        && bizVersion.equalsIgnoreCase(version)) {
                        return FastJsonUtils.getString(item, "bizState");
                    }
                }
            }
        }
        return "";
    }

    /**
     * 从 ZK 获取   apps/biz/
     * @param appName
     * @param ip
     * @return
     */
    public ClientResponseModel getBizState(String appName, String ip) throws Exception {
        ClientResponseModel result = new ClientResponseModel();
        String bizPath = SofaDashboardConstants.SOFA_BOOT_CLIENT_ROOT
                         + SofaDashboardConstants.SOFA_BOOT_CLIENT_BIZ;
        CuratorFramework curatorClient = zkCommandClient.getCuratorClient();
        if (curatorClient.checkExists().forPath(bizPath) != null) {
            String bizAppPath = bizPath + SofaDashboardConstants.SEPARATOR + appName
                                + SofaDashboardConstants.SEPARATOR + ip;
            if (curatorClient.checkExists().forPath(bizAppPath) != null) {
                byte[] bytes = curatorClient.getData().forPath(bizAppPath);
                String data = new String(bytes);
                JSONObject json = JSON.parseObject(data);
                String message = FastJsonUtils.getString(json, "message");
                String code = FastJsonUtils.getString(json, "code");
                result.setCode(ResponseCode.valueOf(code));
                result.setMessage(message);
                String bizInfos = FastJsonUtils.getString(json, "bizInfos");
                JSONArray array = JSONArray.parseArray(bizInfos);
                Set<BizModel> bizInfoList = new HashSet<>();
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    BizModel bizModel = new BizModel();
                    bizModel.setBizName(FastJsonUtils.getString(item, "bizName"));
                    bizModel.setBizState(BizState.of(FastJsonUtils.getString(item, "bizState")));
                    bizModel.setBizVersion(FastJsonUtils.getString(item, "bizVersion"));
                    bizModel.setIdentity(FastJsonUtils.getString(item, "identity"));
                    bizModel.setMainClass(FastJsonUtils.getString(item, "mainClass"));
                    bizModel.setClassPath(getUrls("classPath", item));
                    bizModel.setDenyImportClasses(parseBizStateByKey("denyImportClasses", item));
                    bizModel.setDenyImportPackageNodes(parseBizStateByKey("denyImportPackageNodes",
                        item));
                    bizModel.setDenyImportPackageStems(parseBizStateByKey("denyImportPackageStems",
                        item));
                    bizModel.setDenyImportPackages(parseBizStateByKey("denyImportPackages", item));
                    bizModel
                        .setDenyImportResources(parseBizStateByKey("denyImportResources", item));
                    bizModel.setWebContextPath(FastJsonUtils.getString(item, "webContextPath"));
                    bizModel.setPriority(FastJsonUtils.getInteger(item, "priority"));
                    bizModel.setBizClassLoader(getClassLoader(item));
                    bizInfoList.add(bizModel);
                }

                result.setBizInfos(bizInfoList);
            }
        }
        return result;
    }

    private Set<String> parseBizStateByKey(String key, JSONObject json) {
        String val = FastJsonUtils.getString(json, key);
        JSONArray urls = JSONArray.parseArray(val);
        Set<String> data = new HashSet<>();
        for (int i = 0; i < urls.size(); i++) {
            JSONObject jsonObject = urls.getJSONObject(i);
            data.add(jsonObject.toString());
        }
        return data;
    }

    private URL[] getUrls(String key, JSONObject json) throws MalformedURLException {
        String classPath = FastJsonUtils.getString(json, key);
        JSONArray urls = JSONArray.parseArray(classPath);
        URL[] data = new URL[urls.size()];
        for (int i = 0; i < urls.size(); i++) {
            Object object = urls.get(i);
            data[i] = new URL(object == null ? "" : (String) object);

        }
        return data;
    }

    private BizModel.ClassLoader getClassLoader(JSONObject json) {
        BizModel.ClassLoader data = new BizModel.ClassLoader();
        try {
            String parent = FastJsonUtils.getString(json, "bizClassLoader/parent");
            String bizIdentity = FastJsonUtils.getString(json, "bizClassLoader/bizIdentity");
            URL[] urls = getUrls("bizClassLoader/urls", json);
            data.setBizIdentity(bizIdentity);
            data.setParent(parent == null ? "" : parent);
            data.setUrls(urls);
        } catch (Throwable e) {
            LOGGER.error("Error to get bizClassLoader.", e);
        }
        return data;
    }

}
