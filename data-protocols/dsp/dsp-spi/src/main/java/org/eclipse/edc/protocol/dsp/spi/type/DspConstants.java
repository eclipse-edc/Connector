/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.spi.type;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_08_VERSION;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1_VERSION;

/**
 * Dataspace protocol constants.
 */
public interface DspConstants {

    String DSP_CONTEXT_SEPARATOR = ":";
    String DSP_SCOPE = "DSP";
    String DSP_SCOPE_V_08 = DSP_SCOPE + DSP_CONTEXT_SEPARATOR + V_08_VERSION;
    String DSP_SCOPE_V_2024_1 = DSP_SCOPE + DSP_CONTEXT_SEPARATOR + V_2024_1_VERSION;
    String DSP_TRANSFORMER_CONTEXT = "dsp-api";
    String DSP_TRANSFORMER_CONTEXT_V_08 = DSP_TRANSFORMER_CONTEXT + DSP_CONTEXT_SEPARATOR + V_08_VERSION;
    String DSP_TRANSFORMER_CONTEXT_V_2024_1 = DSP_TRANSFORMER_CONTEXT + DSP_CONTEXT_SEPARATOR + V_2024_1_VERSION;

    JsonLdNamespace DSP_NAMESPACE_V_08 = new JsonLdNamespace(DSPACE_SCHEMA);
    JsonLdNamespace DSP_NAMESPACE_V_2024_1 = new JsonLdNamespace(DSPACE_SCHEMA);
}
