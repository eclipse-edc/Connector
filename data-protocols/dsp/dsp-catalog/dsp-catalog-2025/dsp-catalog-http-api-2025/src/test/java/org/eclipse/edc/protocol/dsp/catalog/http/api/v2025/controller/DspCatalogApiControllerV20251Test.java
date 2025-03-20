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

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.dsp.catalog.http.api.controller.DspCatalogApiControllerTestBase;

import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2025_1_PATH;

@ApiTest
public class DspCatalogApiControllerV20251Test extends DspCatalogApiControllerTestBase {

    @Override
    protected String basePath() {
        return V_2025_1_PATH + BASE_PATH;
    }

    @Override
    protected JsonLdNamespace namespace() {
        return DSP_NAMESPACE_V_2025_1;
    }

    @Override
    protected Object controller() {
        return new DspCatalogApiController20251(service, dspRequestHandler, continuationTokenManager);
    }
}
