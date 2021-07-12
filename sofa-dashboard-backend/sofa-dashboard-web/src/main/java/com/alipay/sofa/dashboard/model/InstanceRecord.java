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
package com.alipay.sofa.dashboard.model;

import com.alipay.sofa.dashboard.client.model.common.Application;
import com.alipay.sofa.dashboard.client.model.common.HostAndPort;
import com.alipay.sofa.dashboard.utils.HostPortUtils;

/**
 * @author chen.pengzhi (chpengzh@foxmail.com)
 */
public class InstanceRecord extends Application {

    public InstanceRecord() {
    }

    public InstanceRecord(Application other) {
        setAppName(other.getAppName());
        setHostName(other.getHostName());
        setInternalHost(other.getInternalHost());
        setPort(other.getPort());
        setAppState(other.getAppState());
        setStartTime(other.getStartTime());
        setLastRecover(other.getLastRecover());
    }

    /**
     * Id 是接口层概念，用来和前端交换一个短的 host&port 描述
     *
     * @return 唯一id
     */
    public String getId() {
        return HostPortUtils.uniqueId(new HostAndPort(getHostName(), getInternalHost(), getPort()));
    }

    public void setId(String instanceId) {
        // Do nothing for json serializer
    }

}
