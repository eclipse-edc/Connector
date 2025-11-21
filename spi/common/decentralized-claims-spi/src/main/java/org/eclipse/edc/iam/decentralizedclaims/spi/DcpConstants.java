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

package org.eclipse.edc.iam.decentralizedclaims.spi;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;

public interface DcpConstants {

    @Deprecated(since = "0.14.0")
    String DCP_CONTEXT_URL = "https://w3id.org/tractusx-trust/v0.8";
    @Deprecated(since = "0.14.0")
    String DCP_PREFIX = DCP_CONTEXT_URL + "/";
    @Deprecated(since = "0.14.0")
    JsonLdNamespace DSPACE_DCP_NAMESPACE_V_0_8 = new JsonLdNamespace(DCP_PREFIX);

    String DSPACE_DCP_V_1_0_CONTEXT = "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld";
    JsonLdNamespace DSPACE_DCP_NAMESPACE_V_1_0 = new JsonLdNamespace("https://w3id.org/dspace-dcp/v1.0/");
}
