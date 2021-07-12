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

import com.alipay.sofa.ark.spi.service.ArkService;
import com.alipay.sofa.dashboard.app.AppServiceImpl;
import com.alipay.sofa.dashboard.dao.ArkDao;
import com.alipay.sofa.dashboard.model.*;
import com.alipay.sofa.dashboard.service.ArkMngService;
import com.alipay.sofa.dashboard.utils.SofaDashboardUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * data for ark mng
 *
 * @author: guolei.sgl (guolei.sgl@antfin.com) 18/12/19 上午11:14
 * @since:
 **/
@Service
public class ArkMngServiceImpl implements ArkMngService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArkMngServiceImpl.class);

    @Autowired
    private ArkDao              arkDao;
    @Autowired
    private ZkHelper            zkHelper;

    @Override
    public boolean isRelatedByModuleAndApp(int mId, String appName) {
        return arkDao.queryRelationByModuleIdAndAppName(mId, appName).size() > 0;
    }

    @Override
    public List<ArkPluginModel> fetchRegisteredPlugins() {
        return doFetchPluginsByName(null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean registerPlugin(ArkPluginDO model) {
        try {
            List<ArkPluginDO> arkPluginDOS = arkDao.queryModuleInfoByNameStrict(model
                .getPluginName());
            if (arkPluginDOS.size() > 0) {
                return false;
            }
            // 向模块表中插入一条数据
            arkDao.insert(model);
            int mId = model.getId();
            // 向模板 Owner 表中插入数据
            ArkModuleUserDO arkModuleUserDO = new ArkModuleUserDO();
            arkModuleUserDO.setModuleId(mId);
            // -1 不需要权限校验即可访问使用
            arkModuleUserDO.setUserId(-1);
            arkModuleUserDO.setCreateTime(model.getCreateTime());
            arkDao.insertModuleUser(arkModuleUserDO);
        } catch (Exception e) {
            LOGGER.error("Error to register plugin.", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean updatePlugin(ArkPluginDO model) {
        return arkDao.update(model) > 0;
    }

    @Override
    public boolean addNewVersion(int mId, String version, String address) {
        // 向模板版本表中插入数据
        return doInsertModuleVersion(mId, version, address) > 0;
    }

    @Override
    public boolean deleteVersion(int mId, String version) {
        return arkDao.deletePluginVersion(mId, version) >= 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removePlugins(int mId) {
        try {
            // 移除模块表
            arkDao.remove(mId);
            // 移除模块版本表
            arkDao.removeModuleVersion(mId);
            // 移除模块Owner表
            arkDao.removeModuleUser(mId);
        } catch (Exception e) {
            LOGGER.error("Error to remove plugin.", e);
            return false;
        }
        return true;
    }

    @Override
    public List<ArkPluginModel> fetchPluginsByName(String pluginName) {
        return doFetchPluginsByName(pluginName);
    }

    @Override
    public int relatedAppToPlugin(int moduleId, String appName) {
        if (moduleId < 0) {
            return -1;
        }
        List<String> arkAppList = zkHelper.getArkAppList();
        boolean relatedByModuleAndApp = isRelatedByModuleAndApp(moduleId, appName);
        for (String appApp : arkAppList) {
            //zk中已存在应用 并且 未关联
            if (appApp.equals(appName) && isRelatedByModuleAndApp(moduleId, appName) == false) {
                AppArkDO appArkDO = new AppArkDO();
                appArkDO.setAppName(appName);
                appArkDO.setCreateTime(SofaDashboardUtil.now());
                appArkDO.setModuleId(moduleId);
                arkDao.insertAppArk(appArkDO);
                return appArkDO.getId();
            }
        }
        return -1;
    }

    @Override
    public List<AppArkDO> queryAppsByPlugin(String pluginName) {
        int moduleId = queryModuleIdByPluginName(pluginName);
        return arkDao.queryAppsModuleId(moduleId);
    }

    @Override
    public int cancelRelatedAppToPlugin(String pluginName, String appName) {
        int moduleId = queryModuleIdByPluginName(pluginName);
        return arkDao.deleteByPluginNameAndAppName(moduleId, appName);
    }

    private int queryModuleIdByPluginName(String pluginName) {
        return arkDao.queryModuleIdByPluginName(pluginName);
    }

    private List<ArkPluginModel> doFetchPluginsByName(String pluginName) {
        List<ArkPluginModel> result = new ArrayList<>();
        // 模糊查询所有
        List<ArkPluginDO> list = arkDao.queryModuleInfoByName(pluginName);
        for (ArkPluginDO item : list) {
            ArkPluginModel temp = new ArkPluginModel();
            List<ArkModuleVersionDO> arkModuleVersionDOList = arkDao.queryVersionsByMid(item.getId());
            List<ArkPluginModel.Version> versions = new ArrayList<>();
            arkModuleVersionDOList.forEach(ver -> {
                ArkPluginModel.Version version = new ArkPluginModel.Version();
                version.setSourcePath(ver.getSourcePath());
                version.setVersion(ver.getModuleVersion());
                versions.add(version);
            });
            temp.setVersions(versions);
            temp.setId(item.getId());
            temp.setDescription(item.getDescription());
            temp.setPluginName(item.getPluginName());
            result.add(temp);
        }
        return result;
    }

    /**
     * @param mId
     * @param version
     * @param address
     * @return
     */
    private int doInsertModuleVersion(int mId, String version, String address) {
        // 向模板版本表中插入数据
        ArkModuleVersionDO arkModuleVersionDO = new ArkModuleVersionDO();
        arkModuleVersionDO.setCreateTime(SofaDashboardUtil.now());
        arkModuleVersionDO.setModuleId(mId);
        arkModuleVersionDO.setModuleVersion(version);
        arkModuleVersionDO.setSourcePath(address);
        arkModuleVersionDO.setIsRelease((byte) 0);
        arkDao.insertModuleVersion(arkModuleVersionDO);
        return arkModuleVersionDO.getId();
    }

}
