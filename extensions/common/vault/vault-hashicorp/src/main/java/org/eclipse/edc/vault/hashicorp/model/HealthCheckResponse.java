/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.vault.hashicorp.model;

public class HealthCheckResponse {

    private HealthCheckResponsePayload payload;
    private int code;

    private HealthCheckResponse() {
    }

    public int getCode() {
        return code;
    }

    public HashicorpVaultHealthResponseCode getCodeAsEnum() {
        return switch (code) {
            case 200 -> HashicorpVaultHealthResponseCode.INITIALIZED_UNSEALED_AND_ACTIVE;
            case 429 -> HashicorpVaultHealthResponseCode.UNSEALED_AND_STANDBY;
            case 472 -> HashicorpVaultHealthResponseCode.DISASTER_RECOVERY_MODE_REPLICATION_SECONDARY_AND_ACTIVE;
            case 473 -> HashicorpVaultHealthResponseCode.PERFORMANCE_STANDBY;
            case 501 -> HashicorpVaultHealthResponseCode.NOT_INITIALIZED;
            case 503 -> HashicorpVaultHealthResponseCode.SEALED;
            default -> HashicorpVaultHealthResponseCode.UNSPECIFIED;
        };
    }

    public HealthCheckResponsePayload getPayload() {
        return payload;
    }


    public enum HashicorpVaultHealthResponseCode {
        UNSPECIFIED, // undefined status codes
        INITIALIZED_UNSEALED_AND_ACTIVE, // status code 200
        UNSEALED_AND_STANDBY, // status code 429
        DISASTER_RECOVERY_MODE_REPLICATION_SECONDARY_AND_ACTIVE, // status code 472
        PERFORMANCE_STANDBY, // status code 473
        NOT_INITIALIZED, // status code 501
        SEALED // status code 503
    }

    public static final class Builder {

        private final HealthCheckResponse response;

        private Builder() {
            response = new HealthCheckResponse();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder payload(HealthCheckResponsePayload payload) {
            this.response.payload = payload;
            return this;
        }

        public Builder code(int code) {
            this.response.code = code;
            return this;
        }

        public HealthCheckResponse build() {
            return response;
        }
    }
}
