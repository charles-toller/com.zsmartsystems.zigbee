/**
 * Copyright (c) 2016-2019 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.zigbee.dongle.zstack.api.util;

import com.zsmartsystems.zigbee.IeeeAddress;
import com.zsmartsystems.zigbee.dongle.zstack.api.ZstackFrameRequest;
import com.zsmartsystems.zigbee.dongle.zstack.api.rpc.ZstackRpcSreqErrorSrsp;

/**
 * Class to implement the Z-Stack command <b>UTIL_APSME_LINK_KEY_DATA_GET</b>.
 * <p>
 * This command retrieves APS link key data, Tx and Rx frame counters.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson
 */
public class ZstackUtilApsmeLinkKeyDataGetSreq extends ZstackFrameRequest {

    /**
     * The extended address for which to get the link key data.
     */
    private IeeeAddress extAddr;

    /**
     * Request constructor
     */
    public ZstackUtilApsmeLinkKeyDataGetSreq() {
        synchronousCommand = true;
    }

    /**
     * The extended address for which to get the link key data.
     *
     * @return the current extAddr as {@link IeeeAddress}
     */
    public IeeeAddress getExtAddr() {
        return extAddr;
    }

    /**
     * The extended address for which to get the link key data.
     *
     * @param extAddr the ExtAddr to set as {@link IeeeAddress}
     */
    public void setExtAddr(IeeeAddress extAddr) {
        this.extAddr = extAddr;
    }

    @Override
    public boolean matchSreqError(ZstackRpcSreqErrorSrsp response) {
        return (((response.getReqCmd0() & 0x1F) == ZSTACK_UTIL) && (response.getReqCmd1() == 0x44));
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(ZSTACK_SREQ, ZSTACK_UTIL, 0x44);

        // Serialize the fields
        serializer.serializeIeeeAddress(extAddr);
        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(61);
        builder.append("ZstackUtilApsmeLinkKeyDataGetSreq [extAddr=");
        builder.append(extAddr);
        builder.append(']');
        return builder.toString();
    }
}
