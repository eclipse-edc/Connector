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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial implementation
 *
 */

package org.eclipse.edc.web.spi.exception;

/**
 * Indicates that something happened while proxying a call to an external endpoint
 */
public class BadGatewayException extends EdcApiException {

    public BadGatewayException(String message) {
        super(message);
    }

    @Override
    public String getType() {
        return "BadGateway";
    }
}
