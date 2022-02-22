/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
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


package org.eclipse.dataspaceconnector.spi.types.domain.edr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Describes an endpoint to target in order to access a specific data.
 */
@JsonDeserialize(builder = EndpointDataReference.Builder.class)
public class EndpointDataReference {

    private final String correlationId;
    private final String contractId;
    private final String address;
    private final String authKey;
    private final String authCode;
    private final Long expirationEpochSeconds;

    private EndpointDataReference(@NotNull String correlationId,
                                  @NotNull String contractId,
                                  @NotNull String address,
                                  @NotNull String authKey,
                                  @NotNull String authCode,
                                  Long expirationEpochSeconds) {
        this.correlationId = correlationId;
        this.contractId = contractId;
        this.address = address;
        this.authKey = authKey;
        this.authCode = authCode;
        this.expirationEpochSeconds = expirationEpochSeconds;
    }

    @NotNull
    public String getCorrelationId() {
        return correlationId;
    }

    @NotNull
    public String getContractId() {
        return contractId;
    }

    @NotNull
    public String getAddress() {
        return address;
    }

    @NotNull
    public String getAuthKey() {
        return authKey;
    }

    @NotNull
    public String getAuthCode() {
        return authCode;
    }

    public Long getExpirationEpochSeconds() {
        return expirationEpochSeconds;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String correlationId;
        private String contractId;
        private String address;
        private String authKey;
        private String authCode;
        private Long expirationEpochSeconds;

        private Builder() {
        }

        @JsonCreator
        public static EndpointDataReference.Builder newInstance() {
            return new EndpointDataReference.Builder();
        }

        public EndpointDataReference.Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public EndpointDataReference.Builder contractId(String contractId) {
            this.contractId = contractId;
            return this;
        }

        public EndpointDataReference.Builder address(String address) {
            this.address = address;
            return this;
        }

        public EndpointDataReference.Builder authKey(String apiKey) {
            this.authKey = apiKey;
            return this;
        }

        public EndpointDataReference.Builder authCode(String apiCode) {
            this.authCode = apiCode;
            return this;
        }

        public EndpointDataReference.Builder expirationEpochSeconds(Long expirationEpochSeconds) {
            this.expirationEpochSeconds = expirationEpochSeconds;
            return this;
        }

        public EndpointDataReference build() {
            Objects.requireNonNull(correlationId, "correlationId");
            Objects.requireNonNull(contractId, "contractId");
            Objects.requireNonNull(address, "address");
            Objects.requireNonNull(authKey, "authKey");
            Objects.requireNonNull(authCode, "authCode");

            return new EndpointDataReference(correlationId, contractId, address, authKey, authCode, expirationEpochSeconds);
        }
    }
}
