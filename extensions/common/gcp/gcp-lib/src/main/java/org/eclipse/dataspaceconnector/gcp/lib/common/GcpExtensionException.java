/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.gcp.lib.common;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class GcpExtensionException extends EdcException {

    public GcpExtensionException(String message) {
        super(message);
    }

    public GcpExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GcpExtensionException(Throwable cause) {
        super(cause);
    }

    public GcpExtensionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
