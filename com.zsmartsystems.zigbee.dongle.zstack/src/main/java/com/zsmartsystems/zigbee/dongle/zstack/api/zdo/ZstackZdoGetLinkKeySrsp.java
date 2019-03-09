/**
 * Copyright (c) 2016-2019 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.zigbee.dongle.zstack.api.zdo;

import com.zsmartsystems.zigbee.IeeeAddress;
import com.zsmartsystems.zigbee.dongle.zstack.api.ZstackFrameResponse;
import com.zsmartsystems.zigbee.dongle.zstack.api.ZstackResponseCode;
import com.zsmartsystems.zigbee.security.ZigBeeKey;

/**
 * Class to implement the Z-Stack command <b>ZDO_GET_LINK_KEY</b>.
 * <p>
 * This command retrieves the application link key of a given device.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson
 */
public class ZstackZdoGetLinkKeySrsp extends ZstackFrameResponse {

    /**
     * 0x00 – Success. 0xC8 – Unknown device.
     */
    private ZstackResponseCode status;

    /**
     * Specifies the IEEE address of the pair device of the link key
     */
    private IeeeAddress ieeeAddr;

    /**
     * Link key data of the device
     */
    private ZigBeeKey linkKeyData;

    /**
     * Response and Handler constructor
     */
    public ZstackZdoGetLinkKeySrsp(int[] inputBuffer) {
        // Super creates deserializer and reads header fields
        super(inputBuffer);

        synchronousCommand = true;

        // Deserialize the fields
        status = ZstackResponseCode.valueOf(deserializeUInt8());
        ieeeAddr = deserializeIeeeAddress();
        linkKeyData = deserializeZigBeeKey();
    }

    /**
     * 0x00 – Success. 0xC8 – Unknown device.
     *
     * @return the current status as {@link ZstackResponseCode}
     */
    public ZstackResponseCode getStatus() {
        return status;
    }

    /**
     * 0x00 – Success. 0xC8 – Unknown device.
     *
     * @param status the Status to set as {@link ZstackResponseCode}
     */
    public void setStatus(ZstackResponseCode status) {
        this.status = status;
    }

    /**
     * Specifies the IEEE address of the pair device of the link key
     *
     * @return the current ieeeAddr as {@link IeeeAddress}
     */
    public IeeeAddress getIeeeAddr() {
        return ieeeAddr;
    }

    /**
     * Specifies the IEEE address of the pair device of the link key
     *
     * @param ieeeAddr the IeeeAddr to set as {@link IeeeAddress}
     */
    public void setIeeeAddr(IeeeAddress ieeeAddr) {
        this.ieeeAddr = ieeeAddr;
    }

    /**
     * Link key data of the device
     *
     * @return the current linkKeyData as {@link ZigBeeKey}
     */
    public ZigBeeKey getLinkKeyData() {
        return linkKeyData;
    }

    /**
     * Link key data of the device
     *
     * @param linkKeyData the LinkKeyData to set as {@link ZigBeeKey}
     */
    public void setLinkKeyData(ZigBeeKey linkKeyData) {
        this.linkKeyData = linkKeyData;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(101);
        builder.append("ZstackZdoGetLinkKeySrsp [status=");
        builder.append(status);
        builder.append(", ieeeAddr=");
        builder.append(ieeeAddr);
        builder.append(", linkKeyData=");
        builder.append(linkKeyData);
        builder.append(']');
        return builder.toString();
    }
}
