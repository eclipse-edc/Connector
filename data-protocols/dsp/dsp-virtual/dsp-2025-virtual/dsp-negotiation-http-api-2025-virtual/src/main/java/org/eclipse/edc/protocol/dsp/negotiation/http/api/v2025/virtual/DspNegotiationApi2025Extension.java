/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.http.api.v2025.virtual;

import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.v2025.virtual.controller.DspNegotiationApiController20251;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractAgreementMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractAgreementVerificationMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractNegotiationEventMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractNegotiationTerminationMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractOfferMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractRequestMessageValidator;
import org.eclipse.edc.protocol.dsp.spi.http.DspVirtualSubResourceLocator;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_VERSION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM;

/**
 * Creates and registers the controller for dataspace protocol v2025/1 negotiation requests.
 */
@Extension(value = DspNegotiationApi2025Extension.NAME)
public class DspNegotiationApi2025Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Negotiation Api v2025/1";

    @Inject
    private ContractNegotiationProtocolService protocolService;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private DspRequestHandler dspRequestHandler;
    @Inject
    private DataspaceProfileContextRegistry profileContextRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private ParticipantProfileResolver participantProfileResolver;

    @Inject
    private DspVirtualSubResourceLocator subResourceLocator;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // Register validators for DSP 2025/1 profiles only (other DSP versions are handled by
        // their own extensions).
        profileContextRegistry.addRegistrationCallback(profile -> {
            if (!V_2025_1_VERSION.equals(profile.protocolVersion().version())) {
                return;
            }
            var ns = profile.protocolNamespace();
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM), ContractRequestMessageValidator.instance(ns, false));
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM), ContractOfferMessageValidator.instance(ns, false));
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM), ContractNegotiationEventMessageValidator.instance(ns));
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM), ContractAgreementMessageValidator.instance(ns));
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM), ContractAgreementVerificationMessageValidator.instance(ns));
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM), ContractNegotiationTerminationMessageValidator.instance(ns));
        });

        subResourceLocator.registerSubResource("negotiations", V_2025_1_VERSION,
                new DspNegotiationApiController20251(protocolService, participantContextService, participantProfileResolver, dspRequestHandler));
    }
}
