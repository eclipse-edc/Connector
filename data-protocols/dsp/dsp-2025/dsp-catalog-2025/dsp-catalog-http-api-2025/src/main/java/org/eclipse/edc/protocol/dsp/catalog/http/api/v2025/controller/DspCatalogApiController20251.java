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

package org.eclipse.edc.protocol.dsp.catalog.http.api.v2025.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.protocol.dsp.catalog.http.api.controller.BaseDspCatalogApiController;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2025_1_PATH;

/**
 * Versioned Catalog endpoint for 2024/1 protocol version
 */
@Consumes({APPLICATION_JSON})
@Produces({APPLICATION_JSON})
@Path(V_2025_1_PATH + BASE_PATH)
public class DspCatalogApiController20251 extends BaseDspCatalogApiController {

    public DspCatalogApiController20251(CatalogProtocolService service, DspRequestHandler dspRequestHandler,
                                        ContinuationTokenManager responseDecorator) {
        super(service, dspRequestHandler, responseDecorator, DATASPACE_PROTOCOL_HTTP_V_2025_1, DSP_NAMESPACE_V_2025_1);
    }
}
