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

package org.eclipse.edc.protocol.dsp.catalog.http.api.controller;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;

import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;

class DspCatalogApiController08Test extends DspCatalogApiControllerTestBase {
    
    @Override
    protected String basePath() {
        return BASE_PATH;
    }
    
    @Override
    protected JsonLdNamespace namespace() {
        return DSP_NAMESPACE_V_08;
    }
    
    @Override
    protected Object controller() {
        return new DspCatalogApiController08(service, dspRequestHandler, continuationTokenManager);
    }
}
