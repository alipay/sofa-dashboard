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

import com.alipay.sofa.dashboard.constants.SofaDashboardConstants;
import com.alipay.sofa.dashboard.impl.ZkHelper;
import com.alipay.sofa.dashboard.model.AppArkDO;
import com.alipay.sofa.dashboard.model.AppArkModel;
import com.alipay.sofa.dashboard.model.ArkPluginDO;
import com.alipay.sofa.dashboard.model.ArkPluginModel;
import com.alipay.sofa.dashboard.response.ResponseEntity;
import com.alipay.sofa.dashboard.service.ArkMngService;
import com.alipay.sofa.dashboard.utils.SofaDashboardUtil;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
 * @author: guolei.sgl (guolei.sgl@antfin.com) 18/12/19 上午11:06
 * @since:
 **/
@Api(value = SofaDashboardConstants.API_ARK_TAGS, tags = SofaDashboardConstants.API_ARK_TAGS)
@RestController
@RequestMapping("/api/ark")
public class ArkMngController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArkMngController.class);

    @Autowired
    private ArkMngService       arkMngService;

    @Autowired
    private ZkHelper            zkHelper;

    /**
     * 获取 ark plugin 数据
     *
     * @return
     */
    @ApiOperation(value = "查询全部插件",tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 1)
    @RequestMapping("/plugin-list")
    public List<ArkPluginModel> queryArkPluginList() {
        List<ArkPluginModel> list = arkMngService.fetchRegisteredPlugins();
        list.forEach((item) -> {
            List<AppArkDO> appArkDOS = arkMngService.queryAppsByPlugin(item.getPluginName());
            List<AppArkModel> appArkList = new ArrayList<>();
            appArkDOS.forEach((appArkDO) -> {
                try {
                    AppArkModel appArkModel = new AppArkModel();
                    appArkModel.setAppName(appArkDO.getAppName());
                    appArkModel.setCreateTime(SofaDashboardUtil.formatDate(appArkDO.getCreateTime()));
                    appArkModel.setInstanceNum(zkHelper.getArkAppCount(appArkDO.getAppName()));
                    appArkList.add(appArkModel);
                } catch (Exception e) {
                    LOGGER.error("Failed to calculate ark app count.", e);
                }
            });
            item.setAppArkList(appArkList);
        });
        return list;
    }

    /**
     * register module
     *
     * @param map
     * @return
     */
    @ApiOperation(value = "注册插件", tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 2)
    @RequestMapping("/register")
    public boolean registerPlugin(@RequestBody Map<String, String> map) {
        if (map == null) {
            return false;
        }
        String pluginName = map.get(SofaDashboardConstants.PLUGIN_NAME);
        String description = map.get(SofaDashboardConstants.DESCRIPTION);
        ArkPluginDO arkPluginDO = new ArkPluginDO(pluginName, SofaDashboardUtil.now(), description);
        return arkMngService.registerPlugin(arkPluginDO);
    }

    @ApiOperation(value = "更新插件", tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 3)
    @RequestMapping("/update-plugin")
    public boolean updatePlugin(@RequestBody ArkPluginDO arkPluginDO) {
        if (arkPluginDO == null) {
            return false;
        }
        return arkMngService.updatePlugin(arkPluginDO);
    }

    @ApiOperation(value = "更新插件版本", tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 4)
    @RequestMapping("/register-new-version")
    public boolean registerNewVersion(@RequestBody Map<String, String> map) {
        if (map == null) {
            return false;
        }
        String version = map.get(SofaDashboardConstants.VERSION);
        String id = map.get(SofaDashboardConstants.ID);
        String address = map.get(SofaDashboardConstants.ADDRESS);
        // 不符合
        if (StringUtils.isEmpty(id)) {
            return false;
        }
        return arkMngService.addNewVersion(Integer.valueOf(id), version, address);
    }

    @ApiOperation(value = "删除插件版本", tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 8)
    @RequestMapping("/delete-version")
    public ResponseEntity<Boolean> deleteVersion(@RequestParam("id") int id,
                                                 @RequestParam("version") String version) {
        ResponseEntity<Boolean> result = new ResponseEntity<>();
        if (id <= 0 || StringUtils.isEmpty(version)) {
            result.setSuccess(false);
            result.setError("plugin id or version cannot be blank.");
            return result;
        }

        if (!arkMngService.deleteVersion(id, version)) {
            result.setError("delete version error.");
        }
        return result;
    }

    @ApiOperation(value = "删除插件", tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 9)
    @RequestMapping("/delete-plugin")
    public boolean deletePluginModel(@RequestParam("id") int id) {
        if (id < 0) {
            return false;
        }
        return arkMngService.removePlugins(id);
    }

    @ApiOperation(value = "查询插件", tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 7)
    @RequestMapping("/search-plugin")
    public List<ArkPluginModel> searchPlugins(@RequestParam("pluginName") String pluginName) {
        if (StringUtils.isEmpty(pluginName)) {
            // 返回全部
            return arkMngService.fetchRegisteredPlugins();
        }
        return arkMngService.fetchPluginsByName(pluginName);
    }

    @ApiOperation(value = "关联应用", tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 5)
    @RequestMapping("/related-app")
    public boolean relatedApp(@RequestParam("id") int id, @RequestParam("appName") String appName) {
        return arkMngService.relatedAppToPlugin(id, appName) > 0;
    }

    @ApiOperation(value = "取消关联应用", tags = SofaDashboardConstants.API_ARK_TAGS)
    @ApiOperationSupport(order = 6)
    @RequestMapping("/cancel-related-app")
    public boolean cancelRelatedApp(@RequestParam("pluginName") String pluginName,
                                    @RequestParam("appName") String appName) {
        return arkMngService.cancelRelatedAppToPlugin(pluginName, appName) > 0;
    }
}
