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

package org.eclipse.dataspaceconnector.spi.exception;

import static java.lang.String.format;

public class ObjectNotFoundException extends EdcApiException {

    public ObjectNotFoundException(Class<?> objectType, String objectId) {
        super(format("Object of type %s with ID=%s was not found", objectType.getSimpleName(), objectId));
    }
}
