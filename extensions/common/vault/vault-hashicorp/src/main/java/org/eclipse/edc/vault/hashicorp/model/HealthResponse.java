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

public class HealthResponse {

    private HealthResponsePayload payload;
    private int code;

    private HealthResponse() {
    }

    public int getCode() {
        return code;
    }

    public HashiCorpVaultHealthResponseCode getCodeAsEnum() {
        switch (code) {
            case 200:
                return HashiCorpVaultHealthResponseCode
                        .INITIALIZED_UNSEALED_AND_ACTIVE;
            case 429:
                return HashiCorpVaultHealthResponseCode.UNSEALED_AND_STANDBY;
            case 472:
                return HashiCorpVaultHealthResponseCode
                        .DISASTER_RECOVERY_MODE_REPLICATION_SECONDARY_AND_ACTIVE;
            case 473:
                return HashiCorpVaultHealthResponseCode.PERFORMANCE_STANDBY;
            case 501:
                return HashiCorpVaultHealthResponseCode.NOT_INITIALIZED;
            case 503:
                return HashiCorpVaultHealthResponseCode.SEALED;
            default:
                return HashiCorpVaultHealthResponseCode.UNSPECIFIED;
        }
    }

    public HealthResponsePayload getPayload() {
        return payload;
    }


    public enum HashiCorpVaultHealthResponseCode {
        UNSPECIFIED, // undefined status codes
        INITIALIZED_UNSEALED_AND_ACTIVE, // status code 200
        UNSEALED_AND_STANDBY, // status code 429
        DISASTER_RECOVERY_MODE_REPLICATION_SECONDARY_AND_ACTIVE, // status code 472
        PERFORMANCE_STANDBY, // status code 473
        NOT_INITIALIZED, // status code 501
        SEALED // status code 503
    }

    public static final class Builder {

        private final HealthResponse response;

        private Builder() {
            response = new HealthResponse();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder payload(HealthResponsePayload payload) {
            this.response.payload = payload;
            return this;
        }

        public Builder code(int code) {
            this.response.code = code;
            return this;
        }

        public HealthResponse build() {
            return response;
        }
    }
}
