/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api.v2025.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.transferprocess.http.api.controller.BaseDspTransferProcessApiController;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.BASE_PATH;


/**
 * Versioned Transfer endpoint for 2024/1 protocol version
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(V_2025_1_PATH + BASE_PATH)
public class DspTransferProcessApiController20251 extends BaseDspTransferProcessApiController {

    public DspTransferProcessApiController20251(TransferProcessProtocolService protocolService, DspRequestHandler dspRequestHandler) {
        super(protocolService, dspRequestHandler, DATASPACE_PROTOCOL_HTTP_V_2025_1, DSP_NAMESPACE_V_2025_1);
    }
}
