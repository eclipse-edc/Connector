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

package org.eclipse.edc.connector.api.management.transferprocess.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.NotEmpty;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class TerminateTransferDto {

    public static final String EDC_TERMINATE_TRANSFER_TYPE = EDC_NAMESPACE + "TerminateTransfer";
    public static final String EDC_TERMINATE_TRANSFER_REASON = EDC_NAMESPACE + "reason";

    @NotEmpty(message = "transfer process termination reason cannot be null nor empty")
    private String reason;

    private TerminateTransferDto() {

    }

    public String getReason() {
        return reason;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final TerminateTransferDto instance;

        private Builder() {
            instance = new TerminateTransferDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder reason(String reason) {
            instance.reason = reason;
            return this;
        }

        public TerminateTransferDto build() {
            return instance;
        }
    }
}
