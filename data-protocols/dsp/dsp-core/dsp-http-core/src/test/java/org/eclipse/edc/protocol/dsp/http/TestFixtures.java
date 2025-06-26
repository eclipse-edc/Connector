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

package org.eclipse.edc.protocol.dsp.http;


import org.eclipse.edc.protocol.spi.ProtocolVersion;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_CONTEXT_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT;

public interface TestFixtures {

    String V_MOCK_VERSION = "vX.X";
    String V_MOCK_PATH = "/";
    ProtocolVersion V_MOCK = new ProtocolVersion(V_MOCK_VERSION, V_MOCK_PATH);
    String DSP_TRANSFORMER_CONTEXT_V_MOCK = DSP_TRANSFORMER_CONTEXT + DSP_CONTEXT_SEPARATOR + V_MOCK_VERSION;
}
