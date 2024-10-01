/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http;

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.protocol.dsp.http.dispatcher.DspHttpRemoteMessageDispatcherImpl;
import org.eclipse.edc.protocol.dsp.http.message.DspRequestHandlerImpl;
import org.eclipse.edc.protocol.dsp.http.protocol.DspProtocolParserImpl;
import org.eclipse.edc.protocol.dsp.http.serialization.JsonLdRemoteMessageSerializerImpl;
import org.eclipse.edc.protocol.dsp.http.spi.DspProtocolParser;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.protocol.dsp.http.transform.DspProtocolTypeTransformerRegistryImpl;
import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Provides an implementation of {@link DspHttpRemoteMessageDispatcher} to support sending dataspace
 * protocol messages. The dispatcher can then be used by other extensions to add support for
 * specific message types.
 */
@Extension(value = DspHttpCoreExtension.NAME)
public class DspHttpCoreExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Core Extension";

    /**
     * Policy scope evaluated when a contract negotiation request is made.
     */
    @PolicyScope
    private static final String CONTRACT_NEGOTIATION_REQUEST_SCOPE = "request.contract.negotiation";

    /**
     * Policy scope evaluated when a transfer process request is made.
     */
    @PolicyScope
    private static final String TRANSFER_PROCESS_REQUEST_SCOPE = "request.transfer.process";

    /**
     * Policy scope evaluated when an outgoing catalog request is made
     */
    @PolicyScope
    private static final String CATALOGING_REQUEST_SCOPE = "request.catalog";

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private IdentityService identityService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLdService;
    @Inject(required = false)
    private TokenDecorator decorator;
    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private AudienceResolver audienceResolver;
    @Inject
    private Monitor monitor;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private ProtocolVersionRegistry versionRegistry;

    private DspProtocolTypeTransformerRegistry dspTransformerRegistry;
    private DspProtocolParser dspProtocolParser;


    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DspHttpRemoteMessageDispatcher dspHttpRemoteMessageDispatcher(ServiceExtensionContext context) {
        TokenDecorator td; // either a decorator, or noop
        if (decorator != null) {
            td = decorator;
        } else {
            context.getMonitor().warning("No TokenDecorator was registered. The 'scope' field of outgoing protocol messages will be empty");
            td = bldr -> bldr;
        }

        var dispatcher = new DspHttpRemoteMessageDispatcherImpl(httpClient, identityService, td, policyEngine, audienceResolver);
        registerNegotiationPolicyScopes(dispatcher);
        registerTransferProcessPolicyScopes(dispatcher);
        registerCatalogPolicyScopes(dispatcher);
        dispatcherRegistry.register(DATASPACE_PROTOCOL_HTTP, dispatcher);
        dispatcherRegistry.register(DATASPACE_PROTOCOL_HTTP_V_2024_1, dispatcher);
        return dispatcher;
    }

    @Provider
    public DspRequestHandler dspRequestHandler() {
        return new DspRequestHandlerImpl(monitor, validatorRegistry, dspTransformerRegistry());
    }

    @Provider
    public JsonLdRemoteMessageSerializer jsonLdRemoteMessageSerializer() {
        return new JsonLdRemoteMessageSerializerImpl(dspTransformerRegistry(), typeManager.getMapper(JSON_LD), jsonLdService, DSP_SCOPE);
    }

    @Provider
    public DspProtocolTypeTransformerRegistry dspTransformerRegistry() {
        if (dspTransformerRegistry == null) {
            dspTransformerRegistry = new DspProtocolTypeTransformerRegistryImpl(transformerRegistry, DSP_TRANSFORMER_CONTEXT, dspProtocolParser());
        }
        return dspTransformerRegistry;
    }

    @Provider
    public DspProtocolParser dspProtocolParser() {
        if (dspProtocolParser == null) {
            dspProtocolParser = new DspProtocolParserImpl(versionRegistry);
        }
        return dspProtocolParser;
    }

    private void registerNegotiationPolicyScopes(DspHttpRemoteMessageDispatcher dispatcher) {
        dispatcher.registerPolicyScope(ContractAgreementMessage.class, CONTRACT_NEGOTIATION_REQUEST_SCOPE, ContractRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(ContractNegotiationEventMessage.class, CONTRACT_NEGOTIATION_REQUEST_SCOPE, ContractRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(ContractRequestMessage.class, CONTRACT_NEGOTIATION_REQUEST_SCOPE, ContractRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(ContractNegotiationTerminationMessage.class, CONTRACT_NEGOTIATION_REQUEST_SCOPE, ContractRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(ContractAgreementVerificationMessage.class, CONTRACT_NEGOTIATION_REQUEST_SCOPE, ContractRemoteMessage::getPolicy);
    }

    private void registerTransferProcessPolicyScopes(DspHttpRemoteMessageDispatcher dispatcher) {
        dispatcher.registerPolicyScope(TransferCompletionMessage.class, TRANSFER_PROCESS_REQUEST_SCOPE, TransferRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(TransferSuspensionMessage.class, TRANSFER_PROCESS_REQUEST_SCOPE, TransferRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(TransferTerminationMessage.class, TRANSFER_PROCESS_REQUEST_SCOPE, TransferRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(TransferStartMessage.class, TRANSFER_PROCESS_REQUEST_SCOPE, TransferRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(TransferRequestMessage.class, TRANSFER_PROCESS_REQUEST_SCOPE, TransferRemoteMessage::getPolicy);
    }

    private void registerCatalogPolicyScopes(DspHttpRemoteMessageDispatcher dispatcher) {
        dispatcher.registerPolicyScope(CatalogRequestMessage.class, CATALOGING_REQUEST_SCOPE, CatalogRequestMessage::getPolicy);
        dispatcher.registerPolicyScope(DatasetRequestMessage.class, CATALOGING_REQUEST_SCOPE, DatasetRequestMessage::getPolicy);
    }
}
