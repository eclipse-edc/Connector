/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.response;

/**
 * An operation response status.
 */
public enum ResponseStatus {
    /**
     * The operation completed successfully.
     */
    OK,

    /**
     * The operation errored and should be retried.
     */
    ERROR_RETRY,

    /**
     * The operation errored and should not be retried.
     */
    FATAL_ERROR
}
