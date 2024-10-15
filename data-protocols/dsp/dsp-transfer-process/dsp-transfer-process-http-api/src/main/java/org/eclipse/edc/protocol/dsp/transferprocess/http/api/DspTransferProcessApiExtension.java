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
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_08;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1;

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

    @Override
    public void initialize(ServiceExtensionContext context) {
        validatorRegistry.register(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_IRI, TransferRequestMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_TRANSFER_START_MESSAGE_IRI, TransferStartMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_IRI, TransferCompletionMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_IRI, TransferTerminationMessageValidator.instance());

        webService.registerResource(ApiContext.PROTOCOL, new DspTransferProcessApiController(transferProcessProtocolService, dspRequestHandler));
        webService.registerResource(ApiContext.PROTOCOL, new DspTransferProcessApiController20241(transferProcessProtocolService, dspRequestHandler));

        versionRegistry.register(V_2024_1);
        versionRegistry.register(V_08);
    }
}
