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

package org.eclipse.edc.protocol.dsp.negotiation.http.api;

import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.controller.DspNegotiationApiController;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.controller.DspNegotiationApiController20241;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.validation.ContractAgreementMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.validation.ContractAgreementVerificationMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.validation.ContractNegotiationEventMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.validation.ContractNegotiationTerminationMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.validation.ContractOfferMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.validation.ContractRequestMessageValidator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_08;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1;

/**
 * Creates and registers the controller for dataspace protocol negotiation requests.
 */
@Extension(value = DspNegotiationApiExtension.NAME)
public class DspNegotiationApiExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Negotiation Api";

    @Inject
    private WebService webService;
    @Inject
    private ContractNegotiationProtocolService protocolService;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private DspRequestHandler dspRequestHandler;
    @Inject
    private ProtocolVersionRegistry versionRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE, ContractRequestMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE, ContractOfferMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE, ContractNegotiationEventMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE, ContractAgreementMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE, ContractAgreementVerificationMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE, ContractNegotiationTerminationMessageValidator.instance());

        webService.registerResource(ApiContext.PROTOCOL, new DspNegotiationApiController(protocolService, dspRequestHandler));
        webService.registerResource(ApiContext.PROTOCOL, new DspNegotiationApiController20241(protocolService, dspRequestHandler));

        versionRegistry.register(V_2024_1);
        versionRegistry.register(V_08);
    }
}
