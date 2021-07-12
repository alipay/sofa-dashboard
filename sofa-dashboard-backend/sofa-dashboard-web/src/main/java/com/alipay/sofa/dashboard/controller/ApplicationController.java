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
import com.alipay.sofa.dashboard.model.ApplicationInfo;
import com.alipay.sofa.dashboard.spi.AppService;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author guolei.sgl (guolei.sgl@antfin.com) 18/12/7 下午5:15
 */
@RestController
@Api(value = SofaDashboardConstants.API_APPLICATION_TAGS, tags = SofaDashboardConstants.API_APPLICATION_TAGS)
@RequestMapping("/api/application")
public class ApplicationController {

    @Autowired
    private AppService appService;


    @ApiOperation(value = "获取应用信息")
    @GetMapping
    public List<ApplicationInfo> getApplication(@RequestParam(value = "keyword", required = false) String keyword) {
        return StringUtils.isEmpty(keyword) ? appService.getAllStatistics() : appService
            .getStatisticsByKeyword(keyword);
    }
}
