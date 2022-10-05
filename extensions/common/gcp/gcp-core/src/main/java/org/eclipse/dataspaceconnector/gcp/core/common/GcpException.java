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

package org.eclipse.dataspaceconnector.gcp.core.common;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class GcpException extends EdcException {

    public GcpException(String message) {
        super(message);
    }

    public GcpException(String message, Throwable cause) {
        super(message, cause);
    }

    public GcpException(Throwable cause) {
        super(cause);
    }

    public GcpException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
