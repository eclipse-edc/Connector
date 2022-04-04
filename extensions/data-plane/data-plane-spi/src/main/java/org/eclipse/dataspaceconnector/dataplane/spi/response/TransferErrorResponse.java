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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.spi.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Error response returned when transfer request failed.
 */
public class TransferErrorResponse {
    private final List<String> errors;

    public TransferErrorResponse(@JsonProperty("errors") List<String> errors) {
        this.errors = errors;
    }

    @JsonProperty("errors")
    public List<String> getErrors() {
        return errors;
    }
}