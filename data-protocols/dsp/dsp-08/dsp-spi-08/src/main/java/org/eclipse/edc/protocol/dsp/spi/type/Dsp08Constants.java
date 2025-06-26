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

package org.eclipse.edc.protocol.dsp.spi.type;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.spi.ProtocolVersion;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_CONTEXT_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT;

public interface Dsp08Constants {

    String V_08_VERSION = "v0.8";
    String V_08_PATH = "/";
    ProtocolVersion V_08 = new ProtocolVersion(V_08_VERSION, V_08_PATH);

    String DSP_SCOPE_V_08 = DSP_SCOPE + DSP_CONTEXT_SEPARATOR + V_08_VERSION;

    String DSP_TRANSFORMER_CONTEXT_V_08 = DSP_TRANSFORMER_CONTEXT + DSP_CONTEXT_SEPARATOR + V_08_VERSION;

    JsonLdNamespace DSP_NAMESPACE_V_08 = new JsonLdNamespace(DSPACE_SCHEMA);
}
