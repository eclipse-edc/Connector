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

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_2025_1_IRI;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_CONTEXT_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_HTTPS_BINDING;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT;

public interface Dsp2025Constants {

    String V_2025_1_VERSION = "2025-1";
    String V_2025_1_PATH = "/" + V_2025_1_VERSION;
    ProtocolVersion V_2025_1 = new ProtocolVersion(V_2025_1_VERSION, V_2025_1_PATH, DSP_HTTPS_BINDING);

    String DSP_SCOPE_V_2025_1 = DSP_SCOPE + DSP_CONTEXT_SEPARATOR + V_2025_1_VERSION;

    String DSP_TRANSFORMER_CONTEXT_V_2025_1 = DSP_TRANSFORMER_CONTEXT + DSP_CONTEXT_SEPARATOR + V_2025_1_VERSION;

    String DATASPACE_PROTOCOL_HTTP_V_2025_1 = DATASPACE_PROTOCOL_HTTP + DATASPACE_PROTOCOL_HTTP_SEPARATOR + V_2025_1_VERSION;

    JsonLdNamespace DSP_NAMESPACE_V_2025_1 = new JsonLdNamespace(DSPACE_2025_1_IRI);

}
