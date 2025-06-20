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

package org.eclipse.edc.protocol.dsp.negotiation.transform.v2025.from;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;

public class TestFunction2025 {

    public static String toIri(String term) {
        return DSP_NAMESPACE_V_2025_1.toIri(term);
    }
}
