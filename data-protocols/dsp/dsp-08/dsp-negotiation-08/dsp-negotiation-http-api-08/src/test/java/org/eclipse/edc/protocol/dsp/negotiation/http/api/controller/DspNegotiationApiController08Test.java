/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.http.api.controller;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.ApiTest;

import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;

@ApiTest
class DspNegotiationApiController08Test extends DspNegotiationApiControllerTestBase {
    
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
        return new DspNegotiationApiController08(protocolService, dspRequestHandler);
    }
}
