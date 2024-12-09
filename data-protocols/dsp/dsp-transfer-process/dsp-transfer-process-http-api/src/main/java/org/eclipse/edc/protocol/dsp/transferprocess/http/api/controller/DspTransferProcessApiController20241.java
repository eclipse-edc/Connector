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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.spi.version.DspVersions;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.BASE_PATH;


/**
 * Versioned Transfer endpoint for 2024/1 protocol version
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(DspVersions.V_2024_1_PATH + BASE_PATH)
public class DspTransferProcessApiController20241 extends BaseDspTransferProcessApiController {

    public DspTransferProcessApiController20241(TransferProcessProtocolService protocolService, DspRequestHandler dspRequestHandler) {
        super(protocolService, dspRequestHandler, DATASPACE_PROTOCOL_HTTP_V_2024_1, DSP_NAMESPACE_V_2024_1);
    }
}
