/**
 * Copyright (c) 2016-2019 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.zsmartsystems.zigbee.dongle.ember;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zsmartsystems.zigbee.ExtendedPanId;
import com.zsmartsystems.zigbee.IeeeAddress;
import com.zsmartsystems.zigbee.ZigBeeBroadcastDestination;
import com.zsmartsystems.zigbee.ZigBeeChannel;
import com.zsmartsystems.zigbee.ZigBeeChannelMask;
import com.zsmartsystems.zigbee.ZigBeeExecutors;
import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.ZigBeeNodeStatus;
import com.zsmartsystems.zigbee.ZigBeeNwkAddressMode;
import com.zsmartsystems.zigbee.ZigBeeProfileType;
import com.zsmartsystems.zigbee.ZigBeeStatus;
import com.zsmartsystems.zigbee.aps.ZigBeeApsFrame;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.EzspFrame;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspChildJoinHandler;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspIncomingMessageHandler;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspLaunchStandaloneBootloaderRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspLaunchStandaloneBootloaderResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMessageSentHandler;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfglibRxHandler;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspNetworkStateRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspSendBroadcastRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspSendBroadcastResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspSendMulticastRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspSendMulticastResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspSendUnicastRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspSendUnicastResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspSetConcentratorRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspSetConcentratorResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspStackStatusHandler;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspTrustCenterJoinHandler;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspVersionResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberApsFrame;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberApsOption;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberConcentratorType;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberCurrentSecurityState;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberKeyStruct;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberKeyStructBitmask;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberKeyType;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberNetworkParameters;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberNetworkStatus;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberOutgoingMessageType;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberStatus;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspConfigId;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspDecisionId;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspPolicyId;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspStatus;
import com.zsmartsystems.zigbee.dongle.ember.internal.EmberFirmwareUpdateHandler;
import com.zsmartsystems.zigbee.dongle.ember.internal.EmberNetworkInitialisation;
import com.zsmartsystems.zigbee.dongle.ember.internal.EmberStackConfiguration;
import com.zsmartsystems.zigbee.dongle.ember.internal.EzspFrameHandler;
import com.zsmartsystems.zigbee.dongle.ember.internal.EzspProtocolHandler;
import com.zsmartsystems.zigbee.dongle.ember.internal.ash.AshFrameHandler;
import com.zsmartsystems.zigbee.dongle.ember.internal.spi.SpiFrameHandler;
import com.zsmartsystems.zigbee.dongle.ember.internal.transaction.EzspSingleResponseTransaction;
import com.zsmartsystems.zigbee.dongle.ember.internal.transaction.EzspTransaction;
import com.zsmartsystems.zigbee.security.ZigBeeKey;
import com.zsmartsystems.zigbee.transport.ConcentratorConfig;
import com.zsmartsystems.zigbee.transport.DeviceType;
import com.zsmartsystems.zigbee.transport.TransportConfig;
import com.zsmartsystems.zigbee.transport.TransportConfigOption;
import com.zsmartsystems.zigbee.transport.TrustCentreJoinMode;
import com.zsmartsystems.zigbee.transport.ZigBeePort;
import com.zsmartsystems.zigbee.transport.ZigBeeTransportFirmwareCallback;
import com.zsmartsystems.zigbee.transport.ZigBeeTransportFirmwareStatus;
import com.zsmartsystems.zigbee.transport.ZigBeeTransportFirmwareUpdate;
import com.zsmartsystems.zigbee.transport.ZigBeeTransportProgressState;
import com.zsmartsystems.zigbee.transport.ZigBeeTransportReceive;
import com.zsmartsystems.zigbee.transport.ZigBeeTransportState;
import com.zsmartsystems.zigbee.transport.ZigBeeTransportTransmit;

/**
 * Implementation of the Silabs Ember NCP (Network Co Processor) EZSP dongle implementation.
 *
 * @author Chris Jackson
 *
 */
public class ZigBeeDongleEzsp implements ZigBeeTransportTransmit, ZigBeeTransportFirmwareUpdate, EzspFrameHandler {

    private static final int POLL_FRAME_ID = EzspNetworkStateRequest.FRAME_ID;
    private static final int WAIT_FOR_ONLINE = 5000;

    /**
     * The {@link Logger}.
     */
    private final Logger logger = LoggerFactory.getLogger(ZigBeeDongleEzsp.class);

    /**
     * The serial port used to connect to the dongle
     */
    private ZigBeePort serialPort;

    /**
     * The protocol handler used to send and receive EZSP packets
     */
    private EzspProtocolHandler frameHandler;

    /**
     * The Ember bootload handler
     */
    private EmberFirmwareUpdateHandler bootloadHandler;

    /**
     * The stack configuration we need for the NCP
     */
    private Map<EzspConfigId, Integer> stackConfiguration;

    /**
     * The stack policies we need for the NCP
     */
    private Map<EzspPolicyId, EzspDecisionId> stackPolicies;

    /**
     * The reference to the receive interface
     */
    private ZigBeeTransportReceive zigbeeTransportReceive;

    /**
     * The current link key as {@link ZigBeeKey}
     */
    private ZigBeeKey linkKey = new ZigBeeKey();

    /**
     * The current network key as {@link ZigBeeKey}
     */
    private ZigBeeKey networkKey = new ZigBeeKey();

    /**
     * The current network parameters as {@link EmberNetworkParameters}
     */
    private EmberNetworkParameters networkParameters = new EmberNetworkParameters();

    /**
     * The IeeeAddress of the Ember NCP
     */
    private IeeeAddress ieeeAddress;

    /**
     * The network address of the Ember NCP
     */
    private Integer nwkAddress;

    /**
     * Defines the type of device we want to be - normally this should be COORDINATOR
     */
    private DeviceType deviceType = DeviceType.COORDINATOR;

    /**
     * The low level protocol to use for this dongle
     */
    private EmberSerialProtocol protocol;

    /**
     * The Ember version used in this system. Set during initialisation and saved in case the client is interested.
     */
    private String versionString = "Unknown";

    /**
     * Boolean that is true when the network is UP
     */
    private boolean networkStateUp = false;

    /**
     * Boolean to hold initialisation state. Set to true after {@link #startup()} completes.
     */
    private boolean initialised = false;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> pollingTimer = null;

    /**
     * The rate at which we will do a status poll if we've not sent any other messages within this period
     */
    private int pollRate = 1000;

    /**
     * The time the last command was sent from the {@link ZigBeeNetworkManager}. This is used by the dongle polling task
     * to not poll if commands are otherwise being sent so as to reduce unnecessary communications with the dongle.
     */
    private long lastSendCommand;

    /**
     * If the dongle is being used with the manufacturing library, then this records the listener to be called when
     * packets are received.
     */
    private EmberMfglibListener mfglibListener;

    /**
     * Create a {@link ZigBeeDongleEzsp} with the default ASH2 frame handler
     *
     * @param serialPort the {@link ZigBeePort} to use for the connection
     */
    public ZigBeeDongleEzsp(final ZigBeePort serialPort) {
        this(serialPort, EmberSerialProtocol.ASH2);
    }

    /**
     * Create a {@link ZigBeeDongleEzsp} with the default ASH frame handler
     *
     * @param serialPort the {@link ZigBeePort} to use for the connection
     * @param protocol the {@link EmberSerialProtocol} to use
     */
    public ZigBeeDongleEzsp(final ZigBeePort serialPort, final EmberSerialProtocol protocol) {
        this.serialPort = serialPort;
        this.protocol = protocol;

        // Define the default configuration
        stackConfiguration = new LinkedHashMap<EzspConfigId, Integer>();
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_SOURCE_ROUTE_TABLE_SIZE, 16);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_SECURITY_LEVEL, 5);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_ADDRESS_TABLE_SIZE, 8);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_TRUST_CENTER_ADDRESS_CACHE_SIZE, 2);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_STACK_PROFILE, 2);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_INDIRECT_TRANSMISSION_TIMEOUT, 7680);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_MAX_HOPS, 30);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_TX_POWER_MODE, 0);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_SUPPORTED_NETWORKS, 1);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_KEY_TABLE_SIZE, 4);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_APPLICATION_ZDO_FLAGS, 0x01);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_MAX_END_DEVICE_CHILDREN, 16);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_APS_UNICAST_MESSAGE_COUNT, 10);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_BROADCAST_TABLE_SIZE, 15);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_NEIGHBOR_TABLE_SIZE, 16);
        stackConfiguration.put(EzspConfigId.EZSP_CONFIG_PACKET_BUFFER_COUNT, 255);

        // Define the default policies
        stackPolicies = new TreeMap<EzspPolicyId, EzspDecisionId>();
        stackPolicies.put(EzspPolicyId.EZSP_TC_KEY_REQUEST_POLICY, EzspDecisionId.EZSP_DENY_TC_KEY_REQUESTS);
        stackPolicies.put(EzspPolicyId.EZSP_TRUST_CENTER_POLICY, EzspDecisionId.EZSP_ALLOW_PRECONFIGURED_KEY_JOINS);
        stackPolicies.put(EzspPolicyId.EZSP_MESSAGE_CONTENTS_IN_CALLBACK_POLICY,
                EzspDecisionId.EZSP_MESSAGE_TAG_ONLY_IN_CALLBACK);
        stackPolicies.put(EzspPolicyId.EZSP_APP_KEY_REQUEST_POLICY, EzspDecisionId.EZSP_DENY_APP_KEY_REQUESTS);

        // stackPolicies.put(EzspPolicyId.EZSP_POLL_HANDLER_POLICY, EzspDecisionId.EZSP_POLL_HANDLER_CALLBACK);
        stackPolicies.put(EzspPolicyId.EZSP_BINDING_MODIFICATION_POLICY,
                EzspDecisionId.EZSP_CHECK_BINDING_MODIFICATIONS_ARE_VALID_ENDPOINT_CLUSTERS);

        networkKey = new ZigBeeKey();

        /*
         * Create the scheduler with a single thread. This ensures that commands sent to the dongle, and the processing
         * of responses is performed in order
         */
        executorService = ZigBeeExecutors.newScheduledThreadPool(1, "EmberDongle");
    }

    /**
     * Update the Ember configuration that will be sent to the dongle during the initialisation.
     * <p>
     * Note that this must be called prior to {@link #initialize()} for the configuration to be effective.
     *
     * @param configId the {@link EzspConfigId} to be updated.
     * @param value the value to set (as {@link Integer}. Setting this to null will remove the configuration Id from the
     *            list of configuration to be sent during NCP initialisation.
     * @return the previously configured value, or null if no value was set for the {@link EzspConfigId}
     */
    public Integer updateDefaultConfiguration(EzspConfigId configId, Integer value) {
        if (value == null) {
            return stackConfiguration.remove(configId);
        }
        return stackConfiguration.put(configId, value);
    }

    /**
     * Update the Ember policies that will be sent to the dongle during the initialisation.
     * <p>
     * Note that this must be called prior to {@link #initialize()} for the configuration to be effective.
     *
     * @param policyId the {@link EzspPolicyId} to be updated
     * @param decisionId the (as {@link EzspDecisionId} to set. Setting this to null will remove the policy from
     *            the list of policies to be sent during NCP initialisation.
     * @return the previously configured {@link EzspDecisionId}, or null if no value was set for the
     *         {@link EzspPolicyId}
     */
    public EzspDecisionId updateDefaultPolicy(EzspPolicyId policyId, EzspDecisionId decisionId) {
        if (policyId == null) {
            return stackPolicies.remove(policyId);
        }
        return stackPolicies.put(policyId, decisionId);
    }

    /**
     * Gets an {@link EmberMfglib} instance that can be used for low level testing of the Ember dongle.
     * <p>
     * This may only be used if the {@link ZigBeeDongleEmber} instance has not been initialized on a ZigBee network.
     *
     * @param mfglibListener a {@link EmberMfglibListener} to receive packets received. May be null.
     * @return the {@link EmberMfglib} instance, or null on error
     */
    public EmberMfglib getEmberMfglib(EmberMfglibListener mfglibListener) {
        if (frameHandler == null && !initialiseEzspProtocol()) {
            return null;
        }

        this.mfglibListener = mfglibListener;

        return new EmberMfglib(frameHandler);
    }

    @Override
    public ZigBeeStatus initialize() {
        logger.debug("EZSP dongle initialize with protocol {}.", protocol);

        if (protocol != EmberSerialProtocol.NONE && !initialiseEzspProtocol()) {
            return ZigBeeStatus.COMMUNICATION_ERROR;
        }

        // Perform any stack configuration
        EmberStackConfiguration stackConfigurer = new EmberStackConfiguration(getEmberNcp());

        Map<EzspConfigId, Integer> configuration = stackConfigurer.getConfiguration(stackConfiguration.keySet());
        for (Entry<EzspConfigId, Integer> config : configuration.entrySet()) {
            logger.debug("Configuration state {} = {}", config.getKey(), config.getValue());
        }

        Map<EzspPolicyId, EzspDecisionId> policies = stackConfigurer.getPolicy(stackPolicies.keySet());
        for (Entry<EzspPolicyId, EzspDecisionId> policy : policies.entrySet()) {
            logger.debug("Policy state {} = {}", policy.getKey(), policy.getValue());
        }

        stackConfigurer.setConfiguration(stackConfiguration);
        configuration = stackConfigurer.getConfiguration(stackConfiguration.keySet());
        for (Entry<EzspConfigId, Integer> config : configuration.entrySet()) {
            logger.debug("Configuration state {} = {}", config.getKey(), config.getValue());
        }

        stackConfigurer.setPolicy(stackPolicies);
        policies = stackConfigurer.getPolicy(stackPolicies.keySet());
        for (Entry<EzspPolicyId, EzspDecisionId> policy : policies.entrySet()) {
            logger.debug("Policy state {} = {}", policy.getKey(), policy.getValue());
        }

        EmberNcp ncp = getEmberNcp();

        ieeeAddress = ncp.getIeeeAddress();
        logger.debug("Ember local IEEE Address is {}", ieeeAddress);

        ncp.getNetworkParameters();

        // Add the endpoint
        ncp.addEndpoint(1, 0, ZigBeeProfileType.ZIGBEE_HOME_AUTOMATION.getKey(), new int[] { 0 }, new int[] { 0 });

        // Now initialise the network
        EmberStatus initResponse = ncp.networkInit();

        networkParameters = ncp.getNetworkParameters().getParameters();
        // print current security state to debug logs
        ncp.getCurrentSecurityState();

        scheduleNetworkStatePolling();

        logger.debug("EZSP dongle initialize done: Initialised {}", initResponse != EmberStatus.EMBER_NOT_JOINED);

        return ZigBeeStatus.SUCCESS;
    }

    @Override
    public ZigBeeStatus startup(boolean reinitialize) {
        logger.debug("EZSP dongle startup.");

        // If ashHandler is null then the serial port didn't initialise
        if (frameHandler == null) {
            logger.error("Initialising Ember Dongle but low level handler is not initialised.");
            return ZigBeeStatus.INVALID_STATE;
        }

        EmberNcp ncp = getEmberNcp();

        // Check if the network is initialised
        EmberNetworkStatus networkState = ncp.getNetworkState();
        logger.debug("EZSP networkStateResponse {}", networkState);

        // If we want to reinitialize the network, then go...
        EmberNetworkInitialisation netInitialiser = new EmberNetworkInitialisation(frameHandler);
        if (reinitialize) {
            logger.debug("Reinitialising Ember NCP network.");
            if (deviceType == DeviceType.COORDINATOR) {
                netInitialiser.formNetwork(networkParameters, linkKey, networkKey);
            } else {
                netInitialiser.joinNetwork(networkParameters, linkKey);
            }
        } else if (deviceType == DeviceType.ROUTER) {
            netInitialiser.rejoinNetwork();
        }
        ncp.getNetworkParameters();

        // Wait for the network to come up
        long timer = System.currentTimeMillis() + WAIT_FOR_ONLINE;
        do {
            networkState = ncp.getNetworkState();
            if (networkState == EmberNetworkStatus.EMBER_JOINED_NETWORK) {
                break;
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                break;
            }
        } while (timer > System.currentTimeMillis());
        logger.debug("EZSP networkState after online wait {}", networkState);

        // Get the security state - mainly for information
        EmberCurrentSecurityState currentSecurityState = ncp.getCurrentSecurityState();
        logger.debug("Current Security State = {}", currentSecurityState);

        EmberStatus txPowerResponse = ncp.setRadioPower(networkParameters.getRadioTxPower());
        if (txPowerResponse != EmberStatus.EMBER_SUCCESS) {
            logger.debug("Setting TX Power to {} resulted in {}", networkParameters.getRadioTxPower(), txPowerResponse);
        }

        int address = ncp.getNwkAddress();
        if (address != 0xFFFE) {
            nwkAddress = address;
        }

        initialised = true;

        return (networkState == EmberNetworkStatus.EMBER_JOINED_NETWORK) ? ZigBeeStatus.SUCCESS
                : ZigBeeStatus.BAD_RESPONSE;
    }

    /**
     * This method schedules sending a status request frame on the interval specified by pollRate. If the frameHandler
     * does not receive a response after a certain amount of retries, the state will be set to OFFLINE.
     * The poll will not be sent if other commands have been sent to the dongle within the pollRate period so as to
     * eliminate any unnecessary traffic with the dongle.
     */
    private void scheduleNetworkStatePolling() {
        if (pollingTimer != null) {
            pollingTimer.cancel(true);
        }

        if (pollRate == 0) {
            return;
        }

        pollingTimer = executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                // Don't poll the state if the network is down
                // or we've sent a command to the dongle within the pollRate
                if (!networkStateUp || (lastSendCommand + pollRate > System.currentTimeMillis())) {
                    return;
                }
                // Don't wait for the response. This is running in a single thread scheduler
                frameHandler.queueFrame(new EzspNetworkStateRequest());
            }
        }, pollRate, pollRate, TimeUnit.MILLISECONDS);
    }

    /**
     * Set the polling rate at which the handler will poll the NCP to ensure it is still responding.
     * If the NCP fails to respond within a fixed time the driver will be set to OFFLINE.
     * <p>
     * Setting the rate to 0 will disable polling
     *
     * @param pollRate the polling rate in milliseconds. 0 will disable polling.
     */
    public void setPollRate(int pollRate) {
        this.pollRate = pollRate;
        scheduleNetworkStatePolling();
    }

    @Override
    public void shutdown() {
        if (frameHandler == null) {
            return;
        }
        logger.debug("Ember NCP Shutdown");
        frameHandler.setClosing();
        frameHandler.close();
        serialPort.close();
        frameHandler = null;

        if (mfglibListener != null) {
            mfglibListener = null;
        }

        if (pollingTimer != null) {
            pollingTimer.cancel(true);
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * Returns an instance of the {@link EmberNcp}
     *
     * @return an instance of the {@link EmberNcp}
     */
    public EmberNcp getEmberNcp() {
        return new EmberNcp(frameHandler);
    }

    @Override
    public IeeeAddress getIeeeAddress() {
        return ieeeAddress;
    }

    @Override
    public Integer getNwkAddress() {
        return nwkAddress;
    }

    @Override
    public void sendCommand(final int msgTag, final ZigBeeApsFrame apsFrame) {
        if (frameHandler == null) {
            return;
        }
        lastSendCommand = System.currentTimeMillis();

        EzspTransaction transaction;

        EmberApsFrame emberApsFrame = new EmberApsFrame();
        emberApsFrame.setClusterId(apsFrame.getCluster());
        emberApsFrame.setProfileId(apsFrame.getProfile());
        emberApsFrame.setSourceEndpoint(apsFrame.getSourceEndpoint());
        emberApsFrame.setDestinationEndpoint(apsFrame.getDestinationEndpoint());
        emberApsFrame.setSequence(apsFrame.getApsCounter());
        emberApsFrame.addOptions(EmberApsOption.EMBER_APS_OPTION_RETRY);
        emberApsFrame.addOptions(EmberApsOption.EMBER_APS_OPTION_ENABLE_ROUTE_DISCOVERY);
        emberApsFrame.addOptions(EmberApsOption.EMBER_APS_OPTION_ENABLE_ADDRESS_DISCOVERY);

        if (apsFrame.getSecurityEnabled()) {
            emberApsFrame.addOptions(EmberApsOption.EMBER_APS_OPTION_ENCRYPTION);
        }

        if (apsFrame.getAddressMode() == ZigBeeNwkAddressMode.DEVICE
                && !ZigBeeBroadcastDestination.isBroadcast(apsFrame.getDestinationAddress())) {
            EzspSendUnicastRequest emberUnicast = new EzspSendUnicastRequest();
            emberUnicast.setIndexOrDestination(apsFrame.getDestinationAddress());
            emberUnicast.setMessageTag(msgTag);
            emberUnicast.setSequenceNumber(apsFrame.getApsCounter());
            emberUnicast.setType(EmberOutgoingMessageType.EMBER_OUTGOING_DIRECT);
            emberUnicast.setApsFrame(emberApsFrame);
            emberUnicast.setMessageContents(apsFrame.getPayload());

            transaction = new EzspSingleResponseTransaction(emberUnicast, EzspSendUnicastResponse.class);
        } else if (apsFrame.getAddressMode() == ZigBeeNwkAddressMode.DEVICE
                && ZigBeeBroadcastDestination.isBroadcast(apsFrame.getDestinationAddress())) {

            EzspSendBroadcastRequest emberBroadcast = new EzspSendBroadcastRequest();
            emberBroadcast.setDestination(apsFrame.getDestinationAddress());
            emberBroadcast.setMessageTag(msgTag);
            emberBroadcast.setSequenceNumber(apsFrame.getApsCounter());
            emberBroadcast.setApsFrame(emberApsFrame);
            emberBroadcast.setRadius(apsFrame.getRadius());
            emberBroadcast.setMessageContents(apsFrame.getPayload());

            transaction = new EzspSingleResponseTransaction(emberBroadcast, EzspSendBroadcastResponse.class);
        } else if (apsFrame.getAddressMode() == ZigBeeNwkAddressMode.GROUP) {
            emberApsFrame.setGroupId(apsFrame.getGroupAddress());

            EzspSendMulticastRequest emberMulticast = new EzspSendMulticastRequest();
            emberMulticast.setApsFrame(emberApsFrame);
            emberMulticast.setHops(apsFrame.getRadius());
            emberMulticast.setNonmemberRadius(apsFrame.getNonMemberRadius());
            emberMulticast.setMessageTag(msgTag);
            emberMulticast.setMessageContents(apsFrame.getPayload());

            transaction = new EzspSingleResponseTransaction(emberMulticast, EzspSendMulticastResponse.class);
        } else {
            logger.debug("EZSP message not sent: {}", apsFrame);
            // ZigBeeGroupAddress groupAddress = (ZigBeeGroupAddress) zclCommand.getDestinationAddress();
            // apsFrame.setGroupId(groupAddress.getGroupId());
            return;
        }

        // The response from the SendXxxcast messages returns the network layer sequence number
        // We need to correlate this with the messageTag
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                frameHandler.sendEzspTransaction(transaction);

                EmberStatus status = null;
                if (transaction.getResponse() instanceof EzspSendUnicastResponse) {
                    status = ((EzspSendUnicastResponse) transaction.getResponse()).getStatus();
                } else if (transaction.getResponse() instanceof EzspSendBroadcastResponse) {
                    status = ((EzspSendBroadcastResponse) transaction.getResponse()).getStatus();
                } else if (transaction.getResponse() instanceof EzspSendMulticastResponse) {
                    status = ((EzspSendMulticastResponse) transaction.getResponse()).getStatus();
                } else {
                    logger.debug("Unable to get response from {} :: {}", transaction.getRequest(),
                            transaction.getResponse());
                    return;
                }

                // If this is EMBER_SUCCESS, then do nothing as the command is still not transmitted.
                // If there was an error, then we let the system know we've failed already!
                if (status == EmberStatus.EMBER_SUCCESS) {
                    return;
                }
                zigbeeTransportReceive.receiveCommandState(msgTag, ZigBeeTransportProgressState.TX_NAK);
            }
        });
    }

    @Override
    public void setZigBeeTransportReceive(ZigBeeTransportReceive zigbeeTransportReceive) {
        this.zigbeeTransportReceive = zigbeeTransportReceive;
    }

    @Override
    public void handlePacket(EzspFrame response) {
        if (response.getFrameId() != POLL_FRAME_ID) {
            logger.debug("RX EZSP: " + response.toString());
        }

        if (response instanceof EzspIncomingMessageHandler) {
            if (nwkAddress == null) {
                logger.debug("Ignoring received frame as stack still initialising");
                return;
            }
            EzspIncomingMessageHandler incomingMessage = (EzspIncomingMessageHandler) response;
            EmberApsFrame emberApsFrame = incomingMessage.getApsFrame();

            ZigBeeApsFrame apsFrame = new ZigBeeApsFrame();
            apsFrame.setApsCounter(emberApsFrame.getSequence());
            apsFrame.setCluster(emberApsFrame.getClusterId());
            apsFrame.setProfile(emberApsFrame.getProfileId());
            apsFrame.setSecurityEnabled(
                    emberApsFrame.getOptions().contains(EmberApsOption.EMBER_APS_OPTION_ENCRYPTION));

            apsFrame.setDestinationAddress(nwkAddress);
            apsFrame.setDestinationEndpoint(emberApsFrame.getDestinationEndpoint());

            apsFrame.setSourceAddress(incomingMessage.getSender());
            apsFrame.setSourceEndpoint(emberApsFrame.getSourceEndpoint());

            apsFrame.setPayload(incomingMessage.getMessageContents());
            zigbeeTransportReceive.receiveCommand(apsFrame);

            return;
        }

        // Message has been completed by the NCP
        if (response instanceof EzspMessageSentHandler) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    EzspMessageSentHandler sentHandler = (EzspMessageSentHandler) response;
                    ZigBeeTransportProgressState sentHandlerState;
                    if (sentHandler.getStatus() == EmberStatus.EMBER_SUCCESS) {
                        sentHandlerState = ZigBeeTransportProgressState.RX_ACK;
                    } else {
                        sentHandlerState = ZigBeeTransportProgressState.RX_NAK;
                    }
                    zigbeeTransportReceive.receiveCommandState(sentHandler.getMessageTag(), sentHandlerState);
                }
            });
            return;
        }

        if (response instanceof EzspStackStatusHandler) {
            switch (((EzspStackStatusHandler) response).getStatus()) {
                case EMBER_NETWORK_BUSY:
                    break;
                case EMBER_NETWORK_DOWN:
                    handleLinkStateChange(false);
                    break;
                case EMBER_NETWORK_UP:
                    handleLinkStateChange(true);
                    break;
                default:
                    break;
            }
            return;
        }

        if (response instanceof EzspTrustCenterJoinHandler) {
            EzspTrustCenterJoinHandler joinHandler = (EzspTrustCenterJoinHandler) response;

            ZigBeeNodeStatus status;
            switch (joinHandler.getStatus()) {
                case EMBER_HIGH_SECURITY_UNSECURED_JOIN:
                case EMBER_STANDARD_SECURITY_UNSECURED_JOIN:
                    status = ZigBeeNodeStatus.UNSECURED_JOIN;
                    break;
                case EMBER_HIGH_SECURITY_UNSECURED_REJOIN:
                case EMBER_STANDARD_SECURITY_UNSECURED_REJOIN:
                    status = ZigBeeNodeStatus.UNSECURED_REJOIN;
                    break;
                case EMBER_HIGH_SECURITY_SECURED_REJOIN:
                case EMBER_STANDARD_SECURITY_SECURED_REJOIN:
                    status = ZigBeeNodeStatus.SECURED_REJOIN;
                    break;
                case EMBER_DEVICE_LEFT:
                    status = ZigBeeNodeStatus.DEVICE_LEFT;
                    break;
                default:
                    logger.debug("Unknown state in trust centre join handler {}", joinHandler.getStatus());
                    return;
            }

            zigbeeTransportReceive.nodeStatusUpdate(status, joinHandler.getNewNodeId(), joinHandler.getNewNodeEui64());
            return;
        }

        if (response instanceof EzspChildJoinHandler) {
            EzspChildJoinHandler joinHandler = (EzspChildJoinHandler) response;
            zigbeeTransportReceive.nodeStatusUpdate(ZigBeeNodeStatus.UNSECURED_JOIN, joinHandler.getChildId(),
                    joinHandler.getChildEui64());
            return;
        }

        if (response instanceof EzspMfglibRxHandler) {
            if (mfglibListener != null) {
                EzspMfglibRxHandler mfglibHandler = (EzspMfglibRxHandler) response;
                mfglibListener.emberMfgLibPacketReceived(mfglibHandler.getLinkQuality(), mfglibHandler.getLinkQuality(),
                        mfglibHandler.getPacketContents());
            }
            return;
        }
    }

    @Override
    public void handleLinkStateChange(final boolean linkState) {
        // Only act on changes to OFFLINE once we have completed initialisation
        // changes to ONLINE have to work during init because they mark the end of the initialisation
        if (!initialised && !linkState || linkState == networkStateUp) {
            return;
        }
        networkStateUp = linkState;

        new Thread("EmberLinkStateChange") {
            @Override
            public void run() {
                if (linkState) {
                    EmberNcp ncp = getEmberNcp();
                    int addr = ncp.getNwkAddress();
                    if (addr != 0xFFFE) {
                        nwkAddress = addr;
                    }
                }
                // Handle link changes and notify framework
                zigbeeTransportReceive
                        .setTransportState(linkState ? ZigBeeTransportState.ONLINE : ZigBeeTransportState.OFFLINE);
            }
        }.start();
    }

    @Override
    public ZigBeeChannel getZigBeeChannel() {
        return ZigBeeChannel.create(networkParameters.getRadioChannel());
    }

    @Override
    public ZigBeeStatus setZigBeeChannel(ZigBeeChannel channel) {
        if ((ZigBeeChannelMask.CHANNEL_MASK_2GHZ & channel.getMask()) == 0) {
            logger.debug("Unable to set channel outside of 2.4GHz channels: {}", channel);
            return ZigBeeStatus.INVALID_ARGUMENTS;
        }
        networkParameters.setRadioChannel(channel.getChannel());
        return ZigBeeStatus.SUCCESS;
    }

    @Override
    public int getZigBeePanId() {
        return networkParameters.getPanId();
    }

    @Override
    public ZigBeeStatus setZigBeePanId(int panId) {
        networkParameters.setPanId(panId);
        return ZigBeeStatus.SUCCESS;
    }

    @Override
    public ExtendedPanId getZigBeeExtendedPanId() {
        return networkParameters.getExtendedPanId();
    }

    @Override
    public ZigBeeStatus setZigBeeExtendedPanId(ExtendedPanId extendedPanId) {
        networkParameters.setExtendedPanId(extendedPanId);
        return ZigBeeStatus.SUCCESS;
    }

    @Override
    public ZigBeeStatus setZigBeeNetworkKey(final ZigBeeKey key) {
        networkKey = key;
        if (networkStateUp) {
            return ZigBeeStatus.INVALID_STATE;
        }
        return ZigBeeStatus.SUCCESS;
    }

    @Override
    public ZigBeeKey getZigBeeNetworkKey() {
        EmberNcp ncp = getEmberNcp();
        EmberKeyStruct key = ncp.getKey(EmberKeyType.EMBER_CURRENT_NETWORK_KEY);

        return emberKeyToZigBeeKey(key);
    }

    @Override
    public ZigBeeStatus setTcLinkKey(ZigBeeKey key) {
        linkKey = key;
        if (networkStateUp) {
            return ZigBeeStatus.INVALID_STATE;
        }
        return ZigBeeStatus.SUCCESS;
    }

    @Override
    public ZigBeeKey getTcLinkKey() {
        EmberNcp ncp = getEmberNcp();
        EmberKeyStruct key = ncp.getKey(EmberKeyType.EMBER_TRUST_CENTER_LINK_KEY);

        return emberKeyToZigBeeKey(key);
    }

    @Override
    public void updateTransportConfig(TransportConfig configuration) {
        for (TransportConfigOption option : configuration.getOptions()) {
            try {
                switch (option) {
                    case CONCENTRATOR_CONFIG:
                        configuration.setResult(option,
                                setConcentrator((ConcentratorConfig) configuration.getValue(option)));
                        break;

                    case INSTALL_KEY:
                        EmberNcp ncp = getEmberNcp();
                        ZigBeeKey nodeKey = (ZigBeeKey) configuration.getValue(option);
                        if (!nodeKey.hasAddress()) {
                            logger.debug("Attempt to set INSTALL_KEY without setting address");
                            configuration.setResult(option, ZigBeeStatus.FAILURE);
                            break;
                        }
                        EmberStatus result = ncp.addTransientLinkKey(nodeKey.getAddress(), nodeKey);

                        configuration.setResult(option,
                                result == EmberStatus.EMBER_SUCCESS ? ZigBeeStatus.SUCCESS : ZigBeeStatus.FAILURE);
                        break;

                    case RADIO_TX_POWER:
                        configuration.setResult(option, setEmberTxPower((int) configuration.getValue(option)));
                        break;

                    case DEVICE_TYPE:
                        deviceType = (DeviceType) configuration.getValue(option);
                        configuration.setResult(option, ZigBeeStatus.SUCCESS);
                        break;

                    case TRUST_CENTRE_LINK_KEY:
                        setTcLinkKey((ZigBeeKey) configuration.getValue(option));
                        configuration.setResult(option, ZigBeeStatus.SUCCESS);
                        break;

                    case TRUST_CENTRE_JOIN_MODE:
                        configuration.setResult(option,
                                setTcJoinMode((TrustCentreJoinMode) configuration.getValue(option)));
                        break;

                    default:
                        configuration.setResult(option, ZigBeeStatus.UNSUPPORTED);
                        logger.debug("Unsupported configuration option \"{}\" in EZSP dongle", option);
                        break;
                }
            } catch (ClassCastException e) {
                configuration.setResult(option, ZigBeeStatus.INVALID_ARGUMENTS);
            }
        }
    }

    private ZigBeeStatus setTcJoinMode(TrustCentreJoinMode joinMode) {
        EzspDecisionId emberJoinMode;
        switch (joinMode) {
            case TC_JOIN_INSECURE:
                emberJoinMode = EzspDecisionId.EZSP_ALLOW_JOINS;
                break;
            case TC_JOIN_SECURE:
                emberJoinMode = EzspDecisionId.EZSP_ALLOW_PRECONFIGURED_KEY_JOINS;
                break;
            case TC_JOIN_DENY:
                emberJoinMode = EzspDecisionId.EZSP_DISALLOW_ALL_JOINS_AND_REJOINS;
                break;
            default:
                return ZigBeeStatus.INVALID_ARGUMENTS;
        }
        return (getEmberNcp().setPolicy(EzspPolicyId.EZSP_TRUST_CENTER_POLICY,
                emberJoinMode) == EzspStatus.EZSP_SUCCESS) ? ZigBeeStatus.SUCCESS : ZigBeeStatus.FAILURE;
    }

    @Override
    public String getVersionString() {
        return versionString;
    }

    private boolean initialiseEzspProtocol() {
        if (frameHandler != null) {
            logger.error("Attempt to initialise Ember dongle when already initialised");
            return false;
        }
        if (!serialPort.open()) {
            logger.error("Unable to open Ember serial port");
            return false;
        }

        switch (protocol) {
            case ASH2:
                frameHandler = new AshFrameHandler(this);
                break;
            case SPI:
                frameHandler = new SpiFrameHandler(this);
                break;
            case NONE:
                return true;
            default:
                logger.error("Unknown Ember serial protocol {}", protocol);
                return false;
        }

        // Connect to the ASH handler and NCP
        frameHandler.start(serialPort);
        frameHandler.connect();

        EmberNcp ncp = getEmberNcp();

        // We MUST send the version command first.
        // Any failure to respond here indicates a failure of the ASH or EZSP layers to initialise
        EzspVersionResponse version = ncp.getVersion(4);
        if (version == null) {
            logger.debug("Version returned null. ASH/EZSP not initialised.");
            return false;
        }

        if (version.getProtocolVersion() != EzspFrame.getEzspVersion()) {
            // The device supports a different version that we current have set
            if (!EzspFrame.setEzspVersion(version.getProtocolVersion())) {
                logger.error("NCP requires unsupported version of EZSP (required = V{}, supported = V{})",
                        version.getProtocolVersion(), EzspFrame.getEzspVersion());
                return false;
            }

            version = ncp.getVersion(EzspFrame.getEzspVersion());
            logger.debug(version.toString());
        }

        StringBuilder builder = new StringBuilder(60);
        builder.append("EZSP Version=");
        builder.append(version.getProtocolVersion());
        builder.append(", Stack Type=");
        builder.append(version.getStackType());
        builder.append(", Stack Version=");
        for (int cnt = 3; cnt >= 0; cnt--) {
            builder.append((version.getStackVersion() >> (cnt * 4)) & 0x0F);
            if (cnt != 0) {
                builder.append('.');
            }
        }
        versionString = builder.toString();

        return true;
    }

    @Override
    public boolean updateFirmware(final InputStream firmware, final ZigBeeTransportFirmwareCallback callback) {
        if (frameHandler != null) {
            logger.debug("Ember Frame Handler is operating in updateFirmware");
            return false;
        }

        zigbeeTransportReceive.setTransportState(ZigBeeTransportState.OFFLINE);
        callback.firmwareUpdateCallback(ZigBeeTransportFirmwareStatus.FIRMWARE_UPDATE_STARTED);

        // Initialise the EZSP protocol so we can start the bootloader
        // If this fails, then we continue on the assumption that the bootloader may already be running
        if (initialiseEzspProtocol()) {
            // Send the bootload command, but ignore the response since there doesn't seem to be one
            // despite what the documentation seems to indicate
            EzspLaunchStandaloneBootloaderRequest bootloadCommand = new EzspLaunchStandaloneBootloaderRequest();
            EzspTransaction bootloadTransaction = frameHandler.sendEzspTransaction(
                    new EzspSingleResponseTransaction(bootloadCommand, EzspLaunchStandaloneBootloaderResponse.class));
            EzspLaunchStandaloneBootloaderResponse bootloadResponse = (EzspLaunchStandaloneBootloaderResponse) bootloadTransaction
                    .getResponse();
            logger.debug(bootloadResponse.toString());
            logger.debug("EZSP bootloadResponse {}", bootloadResponse.getStatus());

            if (bootloadResponse.getStatus() != EzspStatus.EZSP_SUCCESS) {
                callback.firmwareUpdateCallback(ZigBeeTransportFirmwareStatus.FIRMWARE_UPDATE_FAILED);
                logger.debug("EZSP bootload failed: bootloadResponse {}", bootloadResponse.getStatus());
                return false;
            }
        }
        // Stop the handler and close the serial port
        logger.debug("EZSP closing frame handler");
        frameHandler.setClosing();
        serialPort.close();
        frameHandler.close();
        frameHandler = null;

        bootloadHandler = new EmberFirmwareUpdateHandler(this, firmware, serialPort, callback);
        bootloadHandler.startBootload();

        return true;
    }

    // Callback from the bootload handler when the transfer is completed/aborted/failed
    public void bootloadComplete() {
        bootloadHandler = null;
    }

    @Override
    public String getFirmwareVersion() {
        int versionIndex = versionString.indexOf("Stack Version=");
        if (versionIndex == -1) {
            return "";
        }
        return versionString.substring(versionIndex + 14);
    }

    @Override
    public boolean cancelUpdateFirmware() {
        if (bootloadHandler == null) {
            return false;
        }
        bootloadHandler.cancelUpdate();
        return true;
    }

    private ZigBeeStatus setConcentrator(ConcentratorConfig concentratorConfig) {
        EzspSetConcentratorRequest concentratorRequest = new EzspSetConcentratorRequest();
        concentratorRequest.setMinTime(concentratorConfig.getRefreshMinimum());
        concentratorRequest.setMaxTime(concentratorConfig.getRefreshMaximum());
        concentratorRequest.setMaxHops(concentratorConfig.getMaxHops());
        concentratorRequest.setRouteErrorThreshold(concentratorConfig.getMaxFailures());
        concentratorRequest.setDeliveryFailureThreshold(concentratorConfig.getMaxFailures());
        switch (concentratorConfig.getType()) {
            case DISABLED:
                concentratorRequest.setEnable(false);
                break;
            case HIGH_RAM:
                concentratorRequest.setConcentratorType(EmberConcentratorType.EMBER_HIGH_RAM_CONCENTRATOR);
                concentratorRequest.setEnable(true);
                break;
            case LOW_RAM:
                concentratorRequest.setConcentratorType(EmberConcentratorType.EMBER_LOW_RAM_CONCENTRATOR);
                concentratorRequest.setEnable(true);
                break;
            default:
                break;
        }

        EzspTransaction concentratorTransaction = frameHandler.sendEzspTransaction(
                new EzspSingleResponseTransaction(concentratorRequest, EzspSetConcentratorResponse.class));
        EzspSetConcentratorResponse concentratorResponse = (EzspSetConcentratorResponse) concentratorTransaction
                .getResponse();
        logger.debug(concentratorResponse.toString());

        if (concentratorResponse.getStatus() == EzspStatus.EZSP_SUCCESS) {
            return ZigBeeStatus.SUCCESS;
        }
        return ZigBeeStatus.FAILURE;
    }

    /**
     * Set the Ember Radio transmitter power
     *
     * @param txPower the power in dBm
     * @return {@link ZigBeeStatus}
     */
    private ZigBeeStatus setEmberTxPower(int txPower) {
        networkParameters.setRadioTxPower(txPower);

        EmberNcp ncp = getEmberNcp();
        return (ncp.setRadioPower(txPower) == EmberStatus.EMBER_SUCCESS) ? ZigBeeStatus.SUCCESS
                : ZigBeeStatus.BAD_RESPONSE;
    }

    /**
     * Get a map of statistics counters from the dongle
     *
     * @return map of counters
     */
    public Map<String, Long> getCounters() {
        Map<String, Long> counters = new ConcurrentHashMap<String, Long>();

        if (frameHandler != null) {
            counters.putAll(frameHandler.getCounters());
        }

        return counters;
    }

    /**
     * Converts from an {@link EmberKeyStruct} to {@link ZigBeeKey}
     *
     * @param emberKey the {@link EmberKeyStruct} read from the NCP
     * @return the {@link ZigBeeKey} used by the framework
     */
    private ZigBeeKey emberKeyToZigBeeKey(EmberKeyStruct emberKey) {
        ZigBeeKey key = new ZigBeeKey(emberKey.getKey().getContents());
        if (emberKey.getBitmask().contains(EmberKeyStructBitmask.EMBER_KEY_HAS_PARTNER_EUI64)) {
            key.setAddress(emberKey.getPartnerEUI64());
        }
        if (emberKey.getBitmask().contains(EmberKeyStructBitmask.EMBER_KEY_HAS_SEQUENCE_NUMBER)) {
            key.setSequenceNumber(emberKey.getSequenceNumber());
        }
        if (emberKey.getBitmask().contains(EmberKeyStructBitmask.EMBER_KEY_HAS_OUTGOING_FRAME_COUNTER)) {
            key.setOutgoingFrameCounter(emberKey.getOutgoingFrameCounter());
        }
        if (emberKey.getBitmask().contains(EmberKeyStructBitmask.EMBER_KEY_HAS_INCOMING_FRAME_COUNTER)) {
            key.setIncomingFrameCounter(emberKey.getIncomingFrameCounter());
        }

        return key;
    }

    /**
     * Gets the {@link EzspProtocolHandler}
     *
     * @return the {@link EzspProtocolHandler}
     */
    protected EzspProtocolHandler getProtocolHandler() {
        return frameHandler;
    }
}
