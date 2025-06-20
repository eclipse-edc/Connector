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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api.controller;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.ApiTest;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.V_2024_1_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.BASE_PATH;

@ApiTest
class DspTransferProcessApiController20241Test extends DspTransferProcessApiControllerBaseTest {

    @Override
    protected String basePath() {
        return V_2024_1_PATH + BASE_PATH;
    }

    @Override
    protected JsonLdNamespace namespace() {
        return DSP_NAMESPACE_V_2024_1;
    }

    @Override
    protected Object controller() {
        return new DspTransferProcessApiController20241(protocolService, dspRequestHandler);
    }
}
