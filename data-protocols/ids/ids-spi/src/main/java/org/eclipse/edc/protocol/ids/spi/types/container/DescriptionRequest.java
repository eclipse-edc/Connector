/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.ids.spi.types.container;

import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.URI;

public class DescriptionRequest {

    private IdsId id;
    private ClaimToken claimToken;
    private QuerySpec querySpec = QuerySpec.none();
    private URI provider;
    private URI consumer;

    private DescriptionRequest() {

    }

    public IdsId getId() {
        return id;
    }

    public ClaimToken getClaimToken() {
        return claimToken;
    }

    public QuerySpec getQuerySpec() {
        return querySpec;
    }

    public URI getProvider() {
        return provider;
    }

    public URI getConsumer() {
        return consumer;
    }

    public static final class Builder {
        private final DescriptionRequest instance;

        private Builder() {
            instance = new DescriptionRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(IdsId id) {
            instance.id = id;
            return this;
        }

        public Builder claimToken(ClaimToken claimToken) {
            instance.claimToken = claimToken;
            return this;
        }

        public Builder querySpec(QuerySpec querySpec) {
            instance.querySpec = querySpec;
            return this;
        }

        public Builder provider(URI provider) {
            instance.provider = provider;
            return this;
        }

        public Builder consumer(URI consumer) {
            instance.consumer = consumer;
            return this;
        }

        public DescriptionRequest build() {
            return instance;
        }
    }

}
