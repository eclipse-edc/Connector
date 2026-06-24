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

public class NotAuthorizedException extends EdcApiException {
    public NotAuthorizedException() {
        super("This request could not be authorized");
    }

    public NotAuthorizedException(String message) {
        super(message);
    }

    @Override
    public String getType() {
        return "NotAuthorized";
    }
}
