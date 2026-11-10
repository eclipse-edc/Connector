/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.web.spi.exception;

/**
 * Indicates that the request could not be processed now, but the same request may succeed later.
 */
public class ServiceUnavailableException extends EdcApiException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    @Override
    public String getType() {
        return "ServiceUnavailable";
    }
}
