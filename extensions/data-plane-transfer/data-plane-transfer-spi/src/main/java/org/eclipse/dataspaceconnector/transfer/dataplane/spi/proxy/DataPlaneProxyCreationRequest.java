/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.CONTRACT_ID;

/**
 * POJO class for requesting creation of a proxy using Data Plane Public API that wraps a data source.
 */
public class DataPlaneProxyCreationRequest {
    private final String id;
    private final DataAddress address;
    private final Map<String, String> properties;

    private DataPlaneProxyCreationRequest(String id, DataAddress address, Map<String, String> properties) {
        this.id = id;
        this.address = address;
        this.properties = properties;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public DataAddress getAddress() {
        return address;
    }

    @NotNull
    public String getContractId() {
        return properties.get(CONTRACT_ID);
    }

    @NotNull
    public Map<String, String> getProperties() {
        return properties;
    }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private DataAddress address;
        private String contractId;
        private final Map<String, String> properties = new HashMap<>();

        private Builder() {
        }

        @JsonCreator
        public static DataPlaneProxyCreationRequest.Builder newInstance() {
            return new DataPlaneProxyCreationRequest.Builder();
        }

        public DataPlaneProxyCreationRequest.Builder id(String id) {
            this.id = id;
            return this;
        }

        public DataPlaneProxyCreationRequest.Builder address(DataAddress address) {
            this.address = address;
            return this;
        }

        public DataPlaneProxyCreationRequest.Builder contractId(String contractId) {
            this.contractId = contractId;
            return this;
        }

        public DataPlaneProxyCreationRequest.Builder property(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public DataPlaneProxyCreationRequest build() {
            Objects.requireNonNull(address, "address");
            Objects.requireNonNull(contractId, "contractId");
            properties.put(CONTRACT_ID, contractId);
            return new DataPlaneProxyCreationRequest(id, address, properties);
        }
    }
}
