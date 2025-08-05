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

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_CONTEXT_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_HTTPS_BINDING;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT;

@Deprecated(since = "0.14.0")
public interface Dsp2024Constants {

    @Deprecated(since = "0.14.0")
    String DSPACE_SCHEMA_2024_1 = "https://w3id.org/dspace/2024/1/";

    @Deprecated(since = "0.14.0")
    String V_2024_1_VERSION = "2024/1";
    @Deprecated(since = "0.14.0")
    String V_2024_1_PATH = "/" + V_2024_1_VERSION;

    @Deprecated(since = "0.14.0")
    ProtocolVersion V_2024_1 = new ProtocolVersion(V_2024_1_VERSION, V_2024_1_PATH, DSP_HTTPS_BINDING);

    @Deprecated(since = "0.14.0")
    String DSP_SCOPE_V_2024_1 = DSP_SCOPE + DSP_CONTEXT_SEPARATOR + V_2024_1_VERSION;
    @Deprecated(since = "0.14.0")
    String DSP_TRANSFORMER_CONTEXT_V_2024_1 = DSP_TRANSFORMER_CONTEXT + DSP_CONTEXT_SEPARATOR + V_2024_1_VERSION;
    @Deprecated(since = "0.14.0")
    JsonLdNamespace DSP_NAMESPACE_V_2024_1 = new JsonLdNamespace(DSPACE_SCHEMA_2024_1);

    @Deprecated(since = "0.14.0")
    String DATASPACE_PROTOCOL_HTTP_V_2024_1 = DATASPACE_PROTOCOL_HTTP + DATASPACE_PROTOCOL_HTTP_SEPARATOR + V_2024_1_VERSION;

}
