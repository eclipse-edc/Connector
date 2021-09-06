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
package org.eclipse.dataspaceconnector.iam.did.spi.hub.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

/**
 *
 */
@JsonTypeName("CommitQueryRequest")
@JsonDeserialize(builder = CommitQueryRequest.Builder.class)
public class CommitQueryRequest extends AbstractHubRequest {
    private CommitQuery query;

    public CommitQuery getQuery() {
        return query;
    }

    private CommitQueryRequest() {
    }

    public static class Builder extends AbstractHubRequest.Builder<CommitQueryRequest, Builder> {

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder query(CommitQuery query) {
            request.query = query;
            return this;
        }

        public CommitQueryRequest build() {
            verify();
            Objects.requireNonNull(request.query, "query");
            return request;
        }

        private Builder() {
            super(new CommitQueryRequest());
        }

    }
}
