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
package com.alipay.sofa.dashboard.dao;

import com.alipay.sofa.dashboard.model.AppArkDO;
import com.alipay.sofa.dashboard.model.ArkModuleUserDO;
import com.alipay.sofa.dashboard.model.ArkModuleVersionDO;
import com.alipay.sofa.dashboard.model.ArkPluginDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 19/1/10 下午4:07
 * @since:
 **/
@Mapper
public interface ArkDao {

    /**
     * 插件入库
     *
     * @param arkPluginDO
     * @return
     */
    int insert(ArkPluginDO arkPluginDO);

    /**
     * 插件模块版本入库
     *
     * @param arkModuleVersionDO
     * @return
     */
    int insertModuleVersion(ArkModuleVersionDO arkModuleVersionDO);

    /**
     * 模块关联负责人入库
     *
     * @param arkModuleUserDO
     * @return
     */
    int insertModuleUser(ArkModuleUserDO arkModuleUserDO);

    /**
     * 根据插件名查询插件信息
     *
     * @param pluginName
     * @return
     */
    int queryModuleIdByName(String pluginName);

    /**
     * 移除插件
     *
     * @param mId
     */
    void remove(int mId);

    /**
     * 删除插件的所有版本
     *
     * @param mId
     */
    void removeModuleVersion(int mId);

    /**
     * 删除插件所有的关联负责人
     *
     * @param mId
     */
    void removeModuleUser(int mId);

    /**
     * 根据插件名查询插件详情-模糊查询
     *
     * @param pluginName
     * @return
     */
    List<ArkPluginDO> queryModuleInfoByName(@Param("pluginName") String pluginName);

    /**
     * 根据插件名查询插件详情
     *
     * @param pluginName
     * @return
     */
    List<ArkPluginDO> queryModuleInfoByNameStrict(@Param("pluginName") String pluginName);

    /**
     * 根据插件ID 查询所有插件版本
     *
     * @param id
     * @return
     */
    List<ArkModuleVersionDO> queryVersionsByMid(int id);

    /**
     * 插件关联应用信息入库
     *
     * @param appArkDO
     * @return
     */
    int insertAppArk(AppArkDO appArkDO);

    /**
     * 查询所有关联插件的应用
     *
     * @param moduleId
     * @return
     */
    List<AppArkDO> queryAppsModuleId(int moduleId);

    /**
     * 根据模块ID和应用名删除关联应用
     *
     * @param moduleId
     * @param appName
     * @return
     */
    int deleteByPluginNameAndAppName(@Param("moduleId") int moduleId,
                                     @Param("appName") String appName);

    /**
     * 根据插件名查询插件ID
     *
     * @param pluginName
     * @return
     */
    int queryModuleIdByPluginName(String pluginName);

    /**
     * 根据插件版本和插件ID查询插件版本详情
     *
     * @param moduleId
     * @param moduleVersion
     * @return
     */
    ArkModuleVersionDO queryByModuleIdAndModuleVersion(@Param("moduleId") int moduleId,
                                                       @Param("moduleVersion") String moduleVersion);

    /**
     * 更新 ArkPluginDO
     * @param model
     * @return
     */
    int update(ArkPluginDO model);

    /**
     * 删除插件版本
     * @param id
     * @param version
     * @return
     */
    int deletePluginVersion(@Param("id") int id, @Param("version") String version);

    /***
     * 根据模块id和应用名字 获取模块-应用关联信息
     * @param mId
     * @param appName
     * @return
     */
    List<AppArkDO> queryRelationByModuleIdAndAppName(int mId, String appName);
}
