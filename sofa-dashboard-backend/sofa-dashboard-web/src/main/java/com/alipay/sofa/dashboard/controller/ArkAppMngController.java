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

import com.alipay.sofa.dashboard.client.model.common.Application;
import com.alipay.sofa.dashboard.constants.SofaDashboardConstants;
import com.alipay.sofa.dashboard.impl.ZkHelper;
import com.alipay.sofa.dashboard.model.AppModuleModel;
import com.alipay.sofa.dashboard.model.AppUnitModel;
import com.alipay.sofa.dashboard.model.ArkPluginModel;
import com.alipay.sofa.dashboard.model.ClientResponseModel;
import com.alipay.sofa.dashboard.model.CommandRequest;
import com.alipay.sofa.dashboard.response.ResponseEntity;
import com.alipay.sofa.dashboard.service.ArkMngService;
import com.alipay.sofa.dashboard.spi.CommandPushManager;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 18/12/21 下午2:26
 * @since:
 **/

@RestController
@RequestMapping("/api/arkapp")
public class ArkAppMngController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArkAppMngController.class);

    @Autowired
    private ArkMngService       arkMngService;
    @Autowired
    private CommandPushManager  commandPushManager;
    @Autowired
    private ZkHelper            zkHelper;

    @RequestMapping("/ark-app")
    @Deprecated
    public AppModuleModel fetchArkApps(@RequestParam("pluginName") String pluginName,@RequestParam("appName") String appName,
                                       @RequestParam("version") String version) throws Exception {

        AppModuleModel appModuleModel = new AppModuleModel();

        List<String> versions = new ArrayList<>();
        // 如果插件名为空，则返回空
        if (StringUtils.isEmpty(pluginName) || StringUtils.isEmpty(appName)) {
            return appModuleModel;
        }

        appModuleModel.setAppName(appName);
        appModuleModel.setPluginName(pluginName);

        List<ArkPluginModel> arkPluginModels = arkMngService.fetchPluginsByName(pluginName);
        arkPluginModels.forEach((item) ->
                item.getVersions().forEach((versionItem -> {
                    versions.add(versionItem.getVersion());
                }))
        );

        // 版本为空，则插件当前插件的所有版本，然后选择一个默认的
        if (StringUtils.isEmpty(version) && !versions.isEmpty()) {
            version = versions.get(0);
        }
        appModuleModel.setDefaultVersion(version);
        appModuleModel.setVersionList(versions);
        // 获取挂载在zk上的应用信息
        List<AppUnitModel> ipUnitList = zkHelper.getArkAppFromZookeeper(appName, pluginName, version);
        appModuleModel.setIpUnitList(ipUnitList);
        return appModuleModel;
    }

    @RequestMapping("/command")
    public boolean command(@RequestBody Map<String, Object> commandMap) {
        // parse commandMap
        CommandRequest commandRequest = parseCommandRequest(commandMap);
        try {
            commandPushManager.pushCommand(commandRequest);
        } catch (Exception e) {
            // 异常在上层已经输出到日志了，这里仅作为此次操作的是否发生异常的一个标记
            return false;
        }
        return true;
    }

    @RequestMapping("biz-state-detail")
    public ClientResponseModel fetchBizState(@RequestParam("ip") String ip,
                                             @RequestParam("appName") String appName) {
        try {
            return zkHelper.getBizState(appName, ip);
        } catch (Exception e) {
            LOGGER.error("Error to fetchBizState.", e);
        }
        return new ClientResponseModel();
    }

    @RequestMapping("biz-state")
    public ResponseEntity<String> getBizState(@RequestParam("ip") String ip,
                                              @RequestParam("appName") String appName,
                                              @RequestParam("pluginName") String pluginName,
                                              @RequestParam("version") String version) {
        ResponseEntity<String> result = new ResponseEntity<>();
        try {
            result.setData(zkHelper.getAppState(appName, ip, pluginName, version));
        } catch (Exception e) {
            result.setSuccess(false);
            LOGGER.error("Error to getBizState.", e);
        }
        return result;
    }

    private CommandRequest parseCommandRequest(Map<String, Object> commandMap) {
        CommandRequest request = new CommandRequest();
        request.setCommand(commandMap.get(SofaDashboardConstants.COMMAND).toString());
        request.setAppName(commandMap.get(SofaDashboardConstants.APP_NAME).toString());
        request.setPluginName(commandMap.get(SofaDashboardConstants.PLUGIN_NAME).toString());
        request.setPluginVersion(commandMap.get(SofaDashboardConstants.PLUGIN_VERSION).toString());
        if (commandMap.get(SofaDashboardConstants.TARGET_HOSTS) instanceof List) {
            request.setTargetHost((List<String>) commandMap
                .get(SofaDashboardConstants.TARGET_HOSTS));
        }
        request.setDimension(commandMap.get(SofaDashboardConstants.DIMENSION).toString());
        return request;
    }

    private List<AppUnitModel> getAppUnitModel(List<Application> applications) {
        List<AppUnitModel> list = new ArrayList<>();
        if (applications == null || applications.isEmpty()) {
            return list;
        }
        applications.forEach((item) -> {
            AppUnitModel appUnitModel = new AppUnitModel();
            appUnitModel.setIp(item.getHostName());
            appUnitModel.setStatus(item.getAppState());
            list.add(appUnitModel);
        });
        return list;
    }
}
