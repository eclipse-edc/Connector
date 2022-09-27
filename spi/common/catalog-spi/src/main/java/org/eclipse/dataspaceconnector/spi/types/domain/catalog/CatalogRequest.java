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
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A request for a participant's {@link Catalog}.
 */
@JsonDeserialize(builder = CatalogRequest.Builder.class)
public class CatalogRequest implements RemoteMessage {

    public static final String RANGE = "range";

    private final String protocol;
    private final String connectorId;
    private final String connectorAddress;
    private final Range range;
    private final List<Criterion> filter;

    private CatalogRequest(@NotNull String protocol, @NotNull String connectorId, @NotNull String connectorAddress, @Nullable Range range,
            @Nullable List<Criterion> filter) {
        this.protocol = protocol;
        this.connectorId = connectorId;
        this.connectorAddress = connectorAddress;
        this.range = range;
        this.filter = filter;
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

    @NotNull
    public String getConnectorId() {
        return connectorId;
    }

    public Range getRange() {
        return range;
    }

    public List<Criterion> getFilter() {
        return filter;
    }

    public Builder toBuilder() {
        return new Builder(protocol, connectorId, connectorAddress, range, filter);
    }

    public static class Builder {
        private String protocol;
        private String connectorId;
        private String connectorAddress;
        private Range range;
        private List<Criterion> criteria;

        private Builder() {

        }

        private Builder(String protocol, String connectorId, String connectorAddress, Range range, List<Criterion> criteria) {
            this.protocol = protocol;
            this.connectorId = connectorId;
            this.connectorAddress = connectorAddress;
            this.range = range;
            this.criteria = criteria;
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

        public CatalogRequest.Builder filter(List<Criterion> criteria) {
            this.criteria = criteria;
            return this;
        }

        public CatalogRequest build() {
            Objects.requireNonNull(protocol, "protocol");
            Objects.requireNonNull(connectorId, "connectorId");
            Objects.requireNonNull(connectorAddress, "connectorAddress");

            return new CatalogRequest(protocol, connectorId, connectorAddress, range, criteria);
        }
    }
}
