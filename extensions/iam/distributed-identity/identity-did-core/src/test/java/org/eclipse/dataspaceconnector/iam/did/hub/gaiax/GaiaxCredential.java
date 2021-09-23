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
package org.eclipse.dataspaceconnector.iam.did.hub.gaiax;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * A sample GAIA-X credential written to an identity hub by a verifier.
 */
@JsonDeserialize(builder = GaiaxCredential.Builder.class)
public class GaiaxCredential {
    private String region;
    private String companyId;

    public String getRegion() {
        return region;
    }

    public String getCompanyId() {
        return companyId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final GaiaxCredential credential;

        private Builder() {
            credential = new GaiaxCredential();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder region(String region) {
            credential.region = region;
            return this;
        }

        public Builder companyId(String companyId) {
            credential.companyId = companyId;
            return this;
        }

        public GaiaxCredential build() {
            return credential;
        }
    }
}
