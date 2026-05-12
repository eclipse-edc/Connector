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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api.v2025.virtual;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.spi.http.DspVirtualSubResourceLocator;
import org.eclipse.edc.protocol.dsp.transferprocess.http.api.v2025.virtual.controller.DspTransferProcessApiController20251;
import org.eclipse.edc.protocol.dsp.transferprocess.validation.TransferCompletionMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.validation.TransferRequestMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.validation.TransferStartMessageValidator;
import org.eclipse.edc.protocol.dsp.transferprocess.validation.TransferTerminationMessageValidator;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_VERSION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;

/**
 * Creates and registers the controller for dataspace protocol v2025/1 transfer process requests.
 */
@Extension(value = DspTransferProcessApi2025Extension.NAME)
public class DspTransferProcessApi2025Extension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol 2025/1: TransferProcess API Extension";
    @Inject
    private TransferProcessProtocolService transferProcessProtocolService;
    @Inject
    private DspRequestHandler dspRequestHandler;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
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
    public void initialize(ServiceExtensionContext context) {
        // Register validators for DSP 2025/1 profiles only (other DSP versions are handled by
        // their own extensions).
        profileContextRegistry.addRegistrationCallback(profile -> {
            if (!V_2025_1_VERSION.equals(profile.protocolVersion().version())) {
                return;
            }
            var ns = profile.protocolNamespace();
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM), TransferRequestMessageValidator.instance(ns));
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM), TransferStartMessageValidator.instance(ns));
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM), TransferCompletionMessageValidator.instance(ns));
            validatorRegistry.register(ns.toIri(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM), TransferTerminationMessageValidator.instance(ns));
        });

        subResourceLocator.registerSubResource("transfers", V_2025_1_VERSION,
                new DspTransferProcessApiController20251(transferProcessProtocolService, participantContextService, participantProfileResolver, dspRequestHandler));
    }
}
