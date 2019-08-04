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
package com.alipay.sofa.dashboard.controller;

import com.alipay.sofa.dashboard.client.model.common.HostAndPort;
import com.alipay.sofa.dashboard.client.model.env.EnvironmentDescriptor;
import com.alipay.sofa.dashboard.client.model.env.PropertySourceDescriptor;
import com.alipay.sofa.dashboard.client.model.health.HealthDescriptor;
import com.alipay.sofa.dashboard.client.model.info.InfoDescriptor;
import com.alipay.sofa.dashboard.client.model.logger.LoggersDescriptor;
import com.alipay.sofa.dashboard.client.model.mappings.MappingsDescriptor;
import com.alipay.sofa.dashboard.client.model.memory.MemoryDescriptor;
import com.alipay.sofa.dashboard.client.model.thread.ThreadSummaryDescriptor;
import com.alipay.sofa.dashboard.model.InstanceRecord;
import com.alipay.sofa.dashboard.model.RecordResponse;
import com.alipay.sofa.dashboard.model.StampedValueEntity;
import com.alipay.sofa.dashboard.spi.AppService;
import com.alipay.sofa.dashboard.spi.MonitorService;
import com.alipay.sofa.dashboard.utils.HostPortUtils;
import com.alipay.sofa.dashboard.utils.MapUtils;
import com.alipay.sofa.dashboard.utils.TreeNodeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 应用实例控制器
 *
 * @author guolei.sgl (guolei.sgl@antfin.com) 2019/7/11 2:51 PM
 * @author chen.pengzhi (chpengzh@foxmail.com)
 */
@RestController
@RequestMapping("/api/instance")
public class InstanceController {

    @Autowired
    private AppService     applicationService;

    @Autowired
    private MonitorService service;

    @GetMapping
    public List<InstanceRecord> instances(
        @RequestParam(value = "applicationName", required = false) String applicationName) {
        return applicationService.getInstancesByName(applicationName).stream()
            .map(InstanceRecord::new)
            .collect(Collectors.toList());
    }

    @GetMapping("/{instanceId}/env")
    public RecordResponse getEnv(
        @PathVariable("instanceId") String instanceId) {
        HostAndPort hostAndPort = HostPortUtils.getById(instanceId);
        EnvironmentDescriptor descriptor = service.fetchEnvironment(hostAndPort);

        //
        // 注意descriptor可能为空
        //
        Map<String, String> envMap = new HashMap<>();
        for (PropertySourceDescriptor propertySource :
            Optional.ofNullable(descriptor).orElse(new EnvironmentDescriptor())
                .getPropertySources()) {
            propertySource.getProperties().forEach((key, value) ->
                envMap.put(key, String.valueOf(value.getValue())));
        }

        //
        // 接口层重新拼装一次前端需要的数据结构概览
        //
        return RecordResponse.newBuilder()
            .overview("name", envMap.get("spring.application.name"))
            .overview("address",
                String.format("%s:%d", hostAndPort.getHost(), hostAndPort.getPort()))
            .overview("sofa-boot.version", envMap.get("sofa-boot.version"))
            .detail(TreeNodeConverter.convert(descriptor))
            .build();
    }

    @GetMapping("/{instanceId}/info")
    public RecordResponse getInfo(@PathVariable("instanceId") String instanceId) {
        HostAndPort hostAndPort = HostPortUtils.getById(instanceId);
        InfoDescriptor descriptor = service.fetchInfo(hostAndPort);

        //
        // 注意descriptor可能为空
        //
        Map<String, Object> infoMap = MapUtils.toFlatMap(
            Optional.ofNullable(descriptor).orElse(new InfoDescriptor()).getInfo())
            .entrySet().stream()
            .limit(3) // 概况只展示3个元素
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        //
        // 接口层重新拼装一次前端需要的数据结构概览
        //
        return RecordResponse.newBuilder()
            .overview(infoMap)
            .detail(TreeNodeConverter.convert(descriptor))
            .build();
    }

    @GetMapping("/{instanceId}/health")
    public RecordResponse getHealth(@PathVariable("instanceId") String instanceId) {
        HostAndPort hostAndPort = HostPortUtils.getById(instanceId);
        HealthDescriptor descriptor = service.fetchHealth(hostAndPort);

        //
        // 注意descriptor可能为空
        //
        if (descriptor == null) {
            descriptor = new HealthDescriptor();
            descriptor.setStatus("UNKNOWN");
            return RecordResponse.newBuilder()
                .overview("health", descriptor.getStatus())
                .detail(TreeNodeConverter.convert(descriptor))
                .build();
        }

        Map<String, Object> overView = MapUtils.toFlatMap(descriptor.getDetails())
            .entrySet().stream()
            .limit(2)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return RecordResponse.newBuilder()
            .overview("Health", descriptor.getStatus())
            .overview(overView)
            .detail(TreeNodeConverter.convert(descriptor))
            .build();
    }

    @GetMapping("/{instanceId}/loggers")
    public RecordResponse getLoggers(@PathVariable("instanceId") String instanceId) {
        HostAndPort hostAndPort = HostPortUtils.getById(instanceId);
        LoggersDescriptor descriptor = service.fetchLoggers(hostAndPort);

        Map<String, Object> loggersMap = descriptor == null
            ? new HashMap<>()
            : descriptor.getLoggers().entrySet().stream()
            .limit(3) // 概况只展示最多3个元素
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getEffectiveLevel()));

        return RecordResponse.newBuilder()
            .overview(loggersMap)
            .detail(TreeNodeConverter.convert(descriptor))
            .build();
    }

    @GetMapping("/{instanceId}/mappings")
    public RecordResponse getMappings(@PathVariable("instanceId") String instanceId) {
        HostAndPort hostAndPort = HostPortUtils.getById(instanceId);
        MappingsDescriptor descriptor = service.fetchMappings(hostAndPort);

        int servletCount = descriptor == null ? 0 : descriptor.getMappings().values()
            .stream()
            .map(it -> it.getServlets().size())
            .reduce((a, b) -> a + b)
            .orElse(0);
        int servletFilterCount = descriptor == null ? 0 : descriptor.getMappings().values()
            .stream()
            .map(it -> it.getServletFilters().size())
            .reduce((a, b) -> a + b)
            .orElse(0);
        int dispatcherServletCount = descriptor == null ? 0 : descriptor.getMappings().values()
            .stream()
            .map(it -> it.getDispatcherServlet().size())
            .reduce((a, b) -> a + b)
            .orElse(0);

        return RecordResponse.newBuilder()
            .overview("servletCount", String.valueOf(servletCount))
            .overview("servletFilterCount", String.valueOf(servletFilterCount))
            .overview("dispatcherServletCount", String.valueOf(dispatcherServletCount))
            .detail(TreeNodeConverter.convert(descriptor))
            .build();
    }

    @GetMapping("/{instanceId}/memory")
    public List<StampedValueEntity<MemoryDescriptor>> getMemoryRecords(@PathVariable("instanceId") String instanceId) {
        HostAndPort hostAndPort = HostPortUtils.getById(instanceId);
        return service.fetchMemoryInfo(hostAndPort);
    }

    @GetMapping("/{instanceId}/thread")
    public List<StampedValueEntity<ThreadSummaryDescriptor>> getThreadRecords(@PathVariable("instanceId") String instanceId) {
        HostAndPort hostAndPort = HostPortUtils.getById(instanceId);
        return service.fetchThreadInfo(hostAndPort);
    }
}
