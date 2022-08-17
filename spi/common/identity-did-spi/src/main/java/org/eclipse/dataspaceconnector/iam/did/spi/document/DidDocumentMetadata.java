/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.spi.document;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Part of the {@link DidResolveResponse}
 */
public class DidDocumentMetadata {
    Method method;
    List<String> equivalentId;
    String canonicalId;

    @JsonProperty("method")
    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @JsonProperty("equivalentId")
    public List<String> getEquivalentId() {
        return equivalentId;
    }

    public void setEquivalentId(List<String> equivalentId) {
        this.equivalentId = equivalentId;
    }

    @JsonProperty("canonicalId")
    public String getCanonicalId() {
        return canonicalId;
    }

    public void setCanonicalId(String canonicalId) {
        this.canonicalId = canonicalId;
    }
}
