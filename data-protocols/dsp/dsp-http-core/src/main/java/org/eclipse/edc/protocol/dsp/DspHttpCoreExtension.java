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

package org.eclipse.edc.protocol.dsp;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.protocol.dsp.dispatcher.DspHttpRemoteMessageDispatcherImpl;
import org.eclipse.edc.protocol.dsp.serialization.JsonLdRemoteMessageSerializerImpl;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

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
    private static final String CONTRACT_NEGOTIATION_REQUEST_SCOPE = "contract.negotiation.request";

    /**
     * Policy scope evaluated when a transfer process request is made.
     */
    @PolicyScope
    private static final String TRANSFER_PROCESS_REQUEST_SCOPE = "transfer.process.request";

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

        var dispatcher = new DspHttpRemoteMessageDispatcherImpl(httpClient, identityService, td, policyEngine);
        registerNegotiationPolicyScopes(dispatcher);
        registerTransferProcessPolicyScopes(dispatcher);
        dispatcherRegistry.register(dispatcher);
        return dispatcher;
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
        dispatcher.registerPolicyScope(TransferTerminationMessage.class, TRANSFER_PROCESS_REQUEST_SCOPE, TransferRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(TransferStartMessage.class, TRANSFER_PROCESS_REQUEST_SCOPE, TransferRemoteMessage::getPolicy);
        dispatcher.registerPolicyScope(TransferRequestMessage.class, TRANSFER_PROCESS_REQUEST_SCOPE, TransferRemoteMessage::getPolicy);
    }

    @Provider
    public JsonLdRemoteMessageSerializer jsonLdRemoteMessageSerializer() {
        return new JsonLdRemoteMessageSerializerImpl(transformerRegistry, typeManager.getMapper(JSON_LD), jsonLdService);
    }

}
