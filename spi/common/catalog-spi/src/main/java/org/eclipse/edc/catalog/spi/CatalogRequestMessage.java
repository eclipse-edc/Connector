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

package org.eclipse.edc.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A request for a participant's {@link Catalog}.
 */
@JsonDeserialize(builder = CatalogRequestMessage.Builder.class)
public class CatalogRequestMessage implements RemoteMessage {

    private final String protocol;
    @Deprecated(forRemoval = true)
    private final String connectorId;
    private final String connectorAddress;
    private final QuerySpec querySpec;

    private CatalogRequestMessage(@NotNull String protocol, @NotNull String connectorId, @NotNull String connectorAddress,
                                  @Nullable QuerySpec querySpec) {
        this.protocol = protocol;
        this.connectorId = connectorId;
        this.connectorAddress = connectorAddress;
        this.querySpec = querySpec;
    }

    @NotNull
    @Override
    public String getProtocol() {
        return protocol;
    }

    @NotNull
    @Override
    public String getConnectorAddress() {
        return connectorAddress;
    }

    @Deprecated
    @NotNull
    public String getConnectorId() {
        return connectorId;
    }

    public QuerySpec getQuerySpec() {
        return querySpec;
    }

    public Builder toBuilder() {
        return new Builder(protocol, connectorId, connectorAddress, querySpec);
    }

    public static class Builder {
        private String protocol;
        private String connectorId;
        private String connectorAddress;
        private QuerySpec querySpec;

        private Builder() {}

        private Builder(String protocol, String connectorId, String connectorAddress, QuerySpec querySpec) {
            this.protocol = protocol;
            this.connectorId = connectorId;
            this.connectorAddress = connectorAddress;
            this.querySpec = querySpec;
        }

        @JsonCreator
        public static CatalogRequestMessage.Builder newInstance() {
            return new CatalogRequestMessage.Builder();
        }

        public CatalogRequestMessage.Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        @Deprecated
        public CatalogRequestMessage.Builder connectorId(String connectorId) {
            this.connectorId = connectorId;
            return this;
        }

        public CatalogRequestMessage.Builder connectorAddress(String connectorAddress) {
            this.connectorAddress = connectorAddress;
            return this;
        }

        public CatalogRequestMessage.Builder querySpec(QuerySpec querySpec) {
            this.querySpec = querySpec;
            return this;
        }

        public CatalogRequestMessage build() {
            Objects.requireNonNull(protocol, "protocol");
            Objects.requireNonNull(connectorAddress, "connectorAddress");

            return new CatalogRequestMessage(protocol, connectorId, connectorAddress, querySpec);
        }
    }
}
