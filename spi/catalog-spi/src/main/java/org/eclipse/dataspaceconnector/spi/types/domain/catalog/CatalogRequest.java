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

package org.eclipse.dataspaceconnector.spi.types.domain.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A request for a participant's {@link Catalog}.
 */
@JsonDeserialize(builder = CatalogRequest.Builder.class)
public class CatalogRequest implements RemoteMessage {

    private final String protocol;
    private final String connectorId;
    private final String connectorAddress;
    private final Range range;

    private CatalogRequest(@NotNull String protocol, @NotNull String connectorId, @NotNull String connectorAddress, @Nullable Range range) {
        this.protocol = protocol;
        this.connectorId = connectorId;
        this.connectorAddress = connectorAddress;
        this.range = range;
    }

    @NotNull
    @Override
    public String getProtocol() {
        return protocol;
    }

    @NotNull
    public String getConnectorId() {
        return connectorId;
    }

    @NotNull
    public String getConnectorAddress() {
        return connectorAddress;
    }

    public Range getRange() {
        return range;
    }


    public Builder toBuilder() {
        return new Builder(protocol, connectorId, connectorAddress, range);
    }

    public static class Builder {
        private String protocol;
        private String connectorId;
        private String connectorAddress;
        private Range range;

        private Builder() {

        }

        private Builder(String protocol, String connectorId, String connectorAddress, Range range) {
            this.protocol = protocol;
            this.connectorId = connectorId;
            this.connectorAddress = connectorAddress;
            this.range = range;
        }

        @JsonCreator
        public static CatalogRequest.Builder newInstance() {
            return new CatalogRequest.Builder();
        }

        public CatalogRequest.Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public CatalogRequest.Builder connectorId(String connectorId) {
            this.connectorId = connectorId;
            return this;
        }

        public CatalogRequest.Builder connectorAddress(String connectorAddress) {
            this.connectorAddress = connectorAddress;
            return this;
        }

        public CatalogRequest.Builder range(Range range) {
            this.range = range;
            return this;
        }

        public CatalogRequest build() {
            Objects.requireNonNull(protocol, "protocol");
            Objects.requireNonNull(connectorId, "connectorId");
            Objects.requireNonNull(connectorAddress, "connectorAddress");

            return new CatalogRequest(protocol, connectorId, connectorAddress, range);
        }
    }
}
