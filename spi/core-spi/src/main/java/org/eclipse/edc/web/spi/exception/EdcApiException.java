/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.web.spi.exception;

import org.eclipse.edc.spi.EdcException;

import java.util.List;

public abstract class EdcApiException extends EdcException {
    private final List<String> messages;

    public EdcApiException(String message) {
        super(message);
        this.messages = List.of(message);
    }

    public EdcApiException(List<String> messages) {
        super(String.join(",", messages));
        this.messages = messages;
    }

    public abstract String getType();

    public List<String> getMessages() {
        return this.messages;
    }
}
