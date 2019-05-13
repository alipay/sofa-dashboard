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
package com.alipay.sofa.dashboard;

import com.alipay.sofa.dashboard.cache.RegistryDataCache;
import com.alipay.sofa.dashboard.domain.RpcProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/4/10 2:06 PM
 * @since:
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SofaAdminServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ServiceManageControllerTest {

    @Autowired
    private RegistryDataCache    registryDataCache;

    @LocalServerPort
    protected int                definedPort;

    private static TestingServer server;

    protected CuratorFramework   client;

    protected RestTemplate       restTemplate;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        server = new TestingServer(2181, true);
        server.start();

    }

    @AfterClass
    public static void tearDownAfterClass() throws IOException {
        server.stop();
    }

    @Before
    public void before() throws Exception {
        restTemplate = new RestTemplate();
        // 初始化 zk 节点
        client = CuratorFrameworkFactory.newClient("localhost:2181", new ExponentialBackoffRetry(
            1000, 3));
        client.start();
        initZookeeperRpcData();
    }

    @After
    public void after() {
        client.close();
    }

    @Test
    public void testQueryServiceList() {
        String request = "http://localhost:" + definedPort + "/api/service/all";
        ArrayList list = restTemplate.getForObject(request, ArrayList.class);
        Assert.assertTrue(list != null && list.size() == 1);
    }

    @Test
    public void testQueryServiceConsumers() {
        // no consumers exist
        String request = "http://localhost:" + definedPort
                         + "/api/service/query/consumers?dataid={1}";
        ArrayList list = restTemplate.getForObject(request, ArrayList.class, "serviceId1");
        Assert.assertNull(list);
    }

    @Test
    public void testQueryService() {
        String request = "http://localhost:" + definedPort
                         + "/api/service/query/services?serviceName={1}";
        ArrayList list = restTemplate.getForObject(request, ArrayList.class, "serviceId1");
        Assert.assertTrue(list != null && list.size() == 1);
    }

    @Test
    public void testQueryServiceProviders() throws InterruptedException {
        // wait for read data from zk
        Thread.sleep(1000);
        // mock providers
        List<RpcProvider> providerList = new ArrayList<>();
        RpcProvider provider = new RpcProvider();
        provider.setServiceName("serviceId1");
        providerList.add(provider);
        registryDataCache.addProviders("serviceId1", providerList);
        String request = "http://localhost:" + definedPort
                         + "/api/service/query/providers?dataid={1}";
        ArrayList list = restTemplate.getForObject(request, ArrayList.class, "serviceId1");
        Assert.assertTrue(list != null && list.size() == 1);
    }

    private void initZookeeperRpcData() throws Exception {
        createNode("/sofa-rpc", null, CreateMode.PERSISTENT);
        createNode("/sofa-rpc/serviceId1/consumers", null, CreateMode.EPHEMERAL);
        String providersData = "/sofa-rpc/serviceId1/providers/bolt%3A%2F%2F127.0.0.1%3A12200%3Fversion%3D1.0%26uniqueId%3D%26timeout%3D3000%26delay%3D-1%26id%3Drpc-cfg-0%26dynamic%3Dtrue%26weight%3D100%26accepts%3D100000%26startTime%3D1550904160657%26appName%3Drpc-provider%26serialization%3Dhessian2%26pid%3D72034%26language%3Djava%26rpcVer%3D50407";
        createNode("/sofa-rpc/serviceId1/providers", providersData.getBytes(), CreateMode.EPHEMERAL);
    }

    private void createNode(String path, byte[] data, CreateMode mode) throws Exception {
        Stat stat = client.checkExists().forPath(path);
        if (stat == null) {
            client.create().creatingParentContainersIfNeeded().withMode(mode).forPath(path, data);
        }
    }
}
