/**
 * Copyright (c) 2016-2019 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.zigbee.dongle.zstack.api.sapi;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to implement the Z-Stack Enumeration <b>ZstackDeviceInformation</b>.
 * <p>
 * Device Info Constants
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson
 */
public enum ZstackDeviceInformation {
    /**
     * Default unknown value
     */
    UNKNOWN(-1),

    /**
     *
     */
    ZB_INFO_DEV_STATE(0x0000),

    /**
     *
     */
    ZB_INFO_IEEE_ADDR(0x0001),

    /**
     *
     */
    ZB_INFO_SHORT_ADDR(0x0002),

    /**
     *
     */
    ZB_INFO_PARENT_SHORT_ADDR(0x0003),

    /**
     *
     */
    ZB_INFO_PARENT_IEEE_ADDR(0x0004),

    /**
     *
     */
    ZB_INFO_CHANNEL(0x0005),

    /**
     *
     */
    ZB_INFO_PAN_ID(0x0006),

    /**
     *
     */
    ZB_INFO_EXT_PAN_ID(0x0007);

    /**
     * A mapping between the integer code and its corresponding type to
     * facilitate lookup by code.
     */
    private static Map<Integer, ZstackDeviceInformation> codeMapping;

    private int key;

    static {
        codeMapping = new HashMap<Integer, ZstackDeviceInformation>();
        for (ZstackDeviceInformation s : values()) {
            codeMapping.put(s.key, s);
        }
    }

    private ZstackDeviceInformation(int key) {
        this.key = key;
    }

    /**
     * Lookup function based on the type code. Returns null if the code does not exist.
     *
     * @param code the code to lookup
     * @return enumeration value of the alarm type.
     */
    public static ZstackDeviceInformation valueOf(int code) {
        if (codeMapping.get(code) == null) {
            return UNKNOWN;
        }

        return codeMapping.get(code);
    }

    /**
     * Returns the Z-Stack protocol defined value for this enumeration.
     *
     * @return the Z-Stack protocol key
     */
    public int getKey() {
        return key;
    }
}
