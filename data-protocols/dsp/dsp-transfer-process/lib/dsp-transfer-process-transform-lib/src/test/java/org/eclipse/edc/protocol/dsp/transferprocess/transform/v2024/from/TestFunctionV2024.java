/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform.v2024.from;

import org.eclipse.edc.protocol.dsp.spi.type.DspConstants;

public class TestFunctionV2024 {

    public static String toIri(String term) {
        return DspConstants.DSP_NAMESPACE_V_2024_1.toIri(term);
    }
}
