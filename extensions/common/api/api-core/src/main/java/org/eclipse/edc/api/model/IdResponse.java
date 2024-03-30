/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.api.model;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represent the response of a "create" call, when a new entity is generated.
 */
public class IdResponse {

    public static final String ID_RESPONSE_TYPE = EDC_NAMESPACE + "IdResponse";
    public static final String ID_RESPONSE_CREATED_AT = EDC_NAMESPACE + "createdAt";

    private String id;
    private long createdAt;

    public String getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public IdResponse() {
    }

    public static final class Builder {

        private final IdResponse idResponse = new IdResponse();

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            idResponse.id = id;
            return this;
        }

        public Builder createdAt(long createdAt) {
            idResponse.createdAt = createdAt;
            return this;
        }

        public IdResponse build() {
            return idResponse;
        }
    }
}
