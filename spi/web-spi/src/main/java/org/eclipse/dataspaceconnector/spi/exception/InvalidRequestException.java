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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.exception;

import java.util.List;

public class InvalidRequestException extends EdcApiException {

    public InvalidRequestException(String message) {
        this(List.of(message));
    }

    public InvalidRequestException(List<String> messages) {
        super(messages);
    }

    @Override
    public String getType() {
        return "InvalidRequest";
    }
}
