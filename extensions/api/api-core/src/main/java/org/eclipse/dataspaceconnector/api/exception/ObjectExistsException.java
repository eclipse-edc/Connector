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

package org.eclipse.dataspaceconnector.api.exception;

import static java.lang.String.format;

public class ObjectExistsException extends EdcApiException {

    public ObjectExistsException(Class<?> objectType, String objectId) {
        super(format("Object of type %s already exists with ID = %s", objectType.getSimpleName(), objectId));
    }
}
