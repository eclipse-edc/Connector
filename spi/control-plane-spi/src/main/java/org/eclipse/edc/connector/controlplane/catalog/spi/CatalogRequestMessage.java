/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A request for a participant's {@link Catalog}.
 */
@JsonDeserialize(builder = CatalogRequestMessage.Builder.class)
public class CatalogRequestMessage extends ProtocolRemoteMessage {

    private List<String> additionalScopes = new ArrayList<>();
    private QuerySpec querySpec;

    private CatalogRequestMessage() {
    }

    public QuerySpec getQuerySpec() {
        return querySpec;
    }

    /**
     * Returns the {@link Policy} associated with the Catalog Request. Currently, this is an empty policy and serves as
     * placeholder in order to execute the scope extraction.
     *
     * @return the stub {@link Policy}.
     */
    @Override
    public Policy getPolicy() {
        return Policy.Builder.newInstance().build();
    }

    public List<String> getAdditionalScopes() {
        return additionalScopes;
    }

    public static class Builder extends ProtocolRemoteMessage.Builder<CatalogRequestMessage, Builder> {

        private Builder() {
            super(new CatalogRequestMessage());
        }

        @JsonCreator
        public static CatalogRequestMessage.Builder newInstance() {
            return new CatalogRequestMessage.Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public CatalogRequestMessage build() {
            if (message.querySpec == null) {
                message.querySpec = QuerySpec.none();
            }

            return super.build();
        }

        public CatalogRequestMessage.Builder querySpec(QuerySpec querySpec) {
            this.message.querySpec = querySpec;
            return this;
        }

        public CatalogRequestMessage.Builder additionalScopes(String... additionalScopes) {
            this.message.additionalScopes = Arrays.asList(additionalScopes);
            return this;
        }

    }
}
