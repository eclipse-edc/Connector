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

import java.util.List;

public class ObjectConflictException extends EdcApiException {

    public ObjectConflictException(String message) {
        super(message);
    }

    public ObjectConflictException(List<String> messages) {
        super(messages);
    }

    @Override
    public String getType() {
        return "ObjectConflict";
    }
}
