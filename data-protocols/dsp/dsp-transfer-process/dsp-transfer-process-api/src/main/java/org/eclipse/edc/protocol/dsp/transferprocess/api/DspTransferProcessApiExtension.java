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

package org.eclipse.edc.protocol.dsp.transferprocess.api;

import org.eclipse.edc.connector.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.protocol.dsp.spi.configuration.DspApiConfiguration;
import org.eclipse.edc.protocol.dsp.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.transferprocess.api.controller.DspTransferProcessApiController;
import org.eclipse.edc.protocol.dsp.transferprocess.api.controller.DspTransferProcessApiController20241;
import org.eclipse.edc.protocol.dsp.transferprocess.api.validation.TransferCompletionMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.api.validation.TransferRequestMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.api.validation.TransferStartMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.api.validation.TransferTerminationMessageValidator;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.version.DspVersions.V_2024_1;

/**
 * Creates and registers the controller for dataspace protocol transfer process requests.
 */
@Extension(value = DspTransferProcessApiExtension.NAME)
public class DspTransferProcessApiExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol: TransferProcess API Extension";
    @Inject
    private DspApiConfiguration config;
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
        validatorRegistry.register(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE, TransferRequestMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_TRANSFER_START_MESSAGE, TransferStartMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE, TransferCompletionMessageValidator.instance());
        validatorRegistry.register(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE, TransferTerminationMessageValidator.instance());

        webService.registerResource(config.getContextAlias(), new DspTransferProcessApiController(transferProcessProtocolService, dspRequestHandler));
        webService.registerResource(config.getContextAlias(), new DspTransferProcessApiController20241(transferProcessProtocolService, dspRequestHandler));

        versionRegistry.register(V_2024_1);
    }
}
