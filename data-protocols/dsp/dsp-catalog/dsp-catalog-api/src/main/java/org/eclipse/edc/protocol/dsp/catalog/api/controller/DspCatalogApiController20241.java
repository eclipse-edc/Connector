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

package org.eclipse.edc.protocol.dsp.catalog.api.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.protocol.dsp.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.version.DspVersions;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.BASE_PATH;

/**
 * Versioned Catalog endpoint, same as {@link DspCatalogApiController} but exposed on the /2024/1 path
 */
@Consumes({APPLICATION_JSON})
@Produces({APPLICATION_JSON})
@Path(DspVersions.V_2024_1_PATH +  BASE_PATH)
public class DspCatalogApiController20241 extends DspCatalogApiController {

    public DspCatalogApiController20241(CatalogProtocolService service, DspRequestHandler dspRequestHandler) {
        super(service, dspRequestHandler);
    }
}
