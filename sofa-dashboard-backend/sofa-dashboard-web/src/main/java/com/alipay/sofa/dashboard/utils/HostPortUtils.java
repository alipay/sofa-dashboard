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
package com.alipay.sofa.dashboard.utils;

import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.alipay.sofa.dashboard.client.model.common.HostAndPort;

/**
 * 实例地址id映射工具类
 *
 * @author chen.pengzhi (chpengzh@foxmail.com)
 */
public final class HostPortUtils {

    /**
     * 0~255
     */
    private static final String  SEGMENT_REG = "((2[0-4]\\d)|(25[0-5])|(1\\d{2})|([1-9]\\d)|(\\d))";

    private static final Pattern IP_PATTERN  = getIpv4Reg();

    /**
     * 工具类隐藏构造方法
     */
    private HostPortUtils() {
    }

    /**
     * 根据 Host and port 获取唯一id
     *
     * @param hostAndPort
     *            地址
     * @return 唯一id：（真实IP+内部IP）编码 & 端口编码
     */
    public static String uniqueId(HostAndPort hostAndPort) {
        long ipSeg = toDigital(hostAndPort.getHost(), 0);
        long portSeg = ((long) hostAndPort.getPort() & 0xFFFF);
        String internalHost = hostAndPort.getInternalHost();
        if (StringUtils.isEmpty(internalHost) == false) {
            long internalIpSeg = toDigital(internalHost, 1);
            return Long.toHexString(ipSeg + internalIpSeg) + "&" + Long.toHexString(portSeg);
        } else {
            return Long.toHexString(ipSeg) + "&" + Long.toHexString(portSeg);
        }
    }

    /**
     * 从id中获取ipv4地址
     *
     * @param uniqueId
     *            唯一id
     * @return ipv4地址
     */
    public static HostAndPort getById(String uniqueId) {
        String[] segment = uniqueId.split("&");
        int port = ((Number) (Long.parseLong(segment[1], 16) & 0xFFFF)).intValue();
        long ipPartSeg = Long.parseLong(segment[0], 16);
        String ipv4 = fromDigital(ipPartSeg & 0xFFFFFFFFL);
        long virtualPart = ipPartSeg >> 32;
        if (virtualPart <= 0) {
            return new HostAndPort(ipv4, null, port);
        } else {
            String virtualIp = fromDigital(virtualPart & 0xFFFFFFFFL);
            return new HostAndPort(ipv4, virtualIp, port);
        }
    }

    /**
     * ipv4正则
     *
     * @return 正则对象
     */
    private static Pattern getIpv4Reg() {
        StringJoiner sj = new StringJoiner("\\.");
        sj.add(SEGMENT_REG);
        sj.add(SEGMENT_REG);
        sj.add(SEGMENT_REG);
        sj.add(SEGMENT_REG);
        return Pattern.compile(sj.toString());
    }

    /**
     * 非法ipv4抛异常
     *
     * @param ipv4
     *            ipv4
     */
    private static void checkIPv4(String ipv4) {
        if (!isLegalV4(ipv4)) {
            throw new IllegalArgumentException("Illegal ipv4 value " + ipv4);
        }
    }

    /**
     * ip转换为对应32bit数字
     *
     * @param ipv4
     *            点分十进制ipv4
     * @param segmentPart
     *            TODO
     * @return ipv4 对应数字
     */
    private static long toDigital(String ipv4, int segmentPart) {
        checkIPv4(ipv4);
        String[] segments = ipv4.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result += Long.parseLong(segments[3 - i]) << ((8 * i) + segmentPart * Integer.SIZE);
        }
        return result;
    }

    /**
     * 检查是否合法ipv4
     *
     * @param ipv4
     *            ipv4地址
     * @return 是否匹配
     */
    private static boolean isLegalV4(String ipv4) {
        return !StringUtils.isEmpty(ipv4) && IP_PATTERN.matcher(ipv4).matches();
    }

    /**
     * 数字转换为string ip格式
     *
     * @param ipv4
     *            32位数字ip
     * @return 点分十进制ip
     */
    public static String fromDigital(Long ipv4) {
        if (ipv4 > 0xFFFFFFFFL || ipv4 < 0) {
            throw new IllegalArgumentException("Illegal ipv4 digital " + ipv4);
        }
        int[] segments = new int[4];
        for (int i = 0; i < 4; i++) {
            segments[3 - i] = ipv4.intValue() & 0xFF;
            ipv4 >>= 8;
        }
        StringJoiner sj = new StringJoiner(".");
        for (int segment : segments) {
            sj.add(String.valueOf(segment));
        }
        return sj.toString();
    }

    public static void main(String[] args) {
        String id = uniqueId(new HostAndPort("192.168.0.104", "8.16.32.64", 38081));
    }
}
