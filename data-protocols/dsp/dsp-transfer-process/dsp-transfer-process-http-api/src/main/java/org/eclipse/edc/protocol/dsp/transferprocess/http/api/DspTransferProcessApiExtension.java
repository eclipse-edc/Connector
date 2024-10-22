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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.transferprocess.http.api.controller.DspTransferProcessApiController;
import org.eclipse.edc.protocol.dsp.transferprocess.http.api.controller.DspTransferProcessApiController20241;
import org.eclipse.edc.protocol.dsp.transferprocess.validation.TransferCompletionMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.validation.TransferRequestMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.validation.TransferStartMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.validation.TransferTerminationMessageValidator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_08;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Creates and registers the controller for dataspace protocol transfer process requests.
 */
@Extension(value = DspTransferProcessApiExtension.NAME)
public class DspTransferProcessApiExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol: TransferProcess API Extension";
    @Inject
    private WebService webService;
    @Inject
    private TransferProcessProtocolService transferProcessProtocolService;
    @Inject
    private DspRequestHandler dspRequestHandler;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private ProtocolVersionRegistry versionRegistry;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonLdMapper = typeManager.getMapper(JSON_LD);

        registerValidators(DSP_NAMESPACE_V_08);
        registerValidators(DSP_NAMESPACE_V_2024_1);

        webService.registerResource(ApiContext.PROTOCOL, new DspTransferProcessApiController(transferProcessProtocolService, dspRequestHandler));
        webService.registerResource(ApiContext.PROTOCOL, new DspTransferProcessApiController20241(transferProcessProtocolService, dspRequestHandler));
        webService.registerDynamicResource(ApiContext.PROTOCOL, DspTransferProcessApiController.class, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, DSP_SCOPE_V_08));
        webService.registerDynamicResource(ApiContext.PROTOCOL, DspTransferProcessApiController20241.class, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, DSP_SCOPE_V_2024_1));

        versionRegistry.register(V_2024_1);
        versionRegistry.register(V_08);
    }

    private void registerValidators(JsonLdNamespace namespace) {
        validatorRegistry.register(namespace.toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM), TransferRequestMessageValidator.instance(namespace));
        validatorRegistry.register(namespace.toIri(DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM), TransferStartMessageValidator.instance(namespace));
        validatorRegistry.register(namespace.toIri(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM), TransferCompletionMessageValidator.instance(namespace));
        validatorRegistry.register(namespace.toIri(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM), TransferTerminationMessageValidator.instance(namespace));
    }
}
