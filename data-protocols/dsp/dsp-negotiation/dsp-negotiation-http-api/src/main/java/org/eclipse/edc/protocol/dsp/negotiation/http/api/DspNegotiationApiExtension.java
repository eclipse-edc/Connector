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
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.controller.DspNegotiationApiController;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.controller.DspNegotiationApiController20241;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractAgreementMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractAgreementVerificationMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractNegotiationEventMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractNegotiationTerminationMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractOfferMessageValidator;
import org.eclipse.edc.protocol.dsp.negotiation.validation.ContractRequestMessageValidator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_08;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

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

    @Inject
    private JsonLd jsonLd;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonLdMapper = typeManager.getMapper(JSON_LD);

        validatorRegistry.register(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_IRI, ContractRequestMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_IRI, ContractOfferMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_IRI, ContractNegotiationEventMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_IRI, ContractAgreementMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_IRI, ContractAgreementVerificationMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_IRI, ContractNegotiationTerminationMessageValidator.instance());

        webService.registerResource(ApiContext.PROTOCOL, new DspNegotiationApiController(protocolService, dspRequestHandler));
        webService.registerResource(ApiContext.PROTOCOL, new DspNegotiationApiController20241(protocolService, dspRequestHandler));
        webService.registerDynamicResource(ApiContext.PROTOCOL, DspNegotiationApiController.class, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, DSP_SCOPE_V_08));
        webService.registerDynamicResource(ApiContext.PROTOCOL, DspNegotiationApiController20241.class, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, DSP_SCOPE_V_2024_1));

        versionRegistry.register(V_2024_1);
        versionRegistry.register(V_08);
    }
}
