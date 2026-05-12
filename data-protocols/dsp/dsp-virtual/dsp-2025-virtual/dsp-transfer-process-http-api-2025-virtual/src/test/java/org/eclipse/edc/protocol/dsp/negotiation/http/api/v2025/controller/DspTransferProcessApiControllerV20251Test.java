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

package org.eclipse.edc.protocol.dsp.negotiation.http.api.v2025.controller;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.protocol.dsp.transferprocess.http.api.controller.DspTransferProcessApiControllerBaseTest;
import org.eclipse.edc.protocol.dsp.transferprocess.http.api.v2025.virtual.controller.DspVirtualTransferProcessApiController20251;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Optional;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_VERSION;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.BASE_PATH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
class DspTransferProcessApiControllerV20251Test extends DspTransferProcessApiControllerBaseTest {

    private static final String PROFILE_ID = V_2025_1_VERSION;
    private static final DataspaceProfileContext PROFILE = new DataspaceProfileContext(
            PROFILE_ID, V_2025_1, () -> "https://example.org/webhook", ct -> "id",
            DSP_NAMESPACE_V_2025_1, List.of("https://example.org/context.jsonld"));

    private final ParticipantContextService participantContextService = mock();
    private final ParticipantProfileResolver profileResolver = mock();
    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("participantContextId")
            .identity("identity")
            .build();

    @BeforeEach
    void setUp() {
        when(participantContextService.getParticipantContext(participantContext.getParticipantContextId()))
                .thenReturn(ServiceResult.success(participantContext));
        when(profileResolver.resolve(participantContext.getParticipantContextId(), PROFILE_ID))
                .thenReturn(Optional.of(PROFILE));
    }

    @Override
    protected String basePath() {
        return "/%s/%s".formatted(participantContext.getParticipantContextId(), PROFILE_ID) + BASE_PATH;
    }

    @Override
    protected JsonLdNamespace namespace() {
        return DSP_NAMESPACE_V_2025_1;
    }

    @Override
    protected Object controller() {
        return new DspVirtualTransferProcessApiController20251(protocolService, participantContextService, profileResolver, dspRequestHandler);
    }

}
