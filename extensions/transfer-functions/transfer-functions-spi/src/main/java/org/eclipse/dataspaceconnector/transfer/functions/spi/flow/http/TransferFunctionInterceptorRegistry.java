/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.functions.spi.flow.http;

import okhttp3.Interceptor;

/**
 * Registers an interceptor to mediate requests to a transfer function endpoint. Interceptors may decorate requests to add headers such as authentication.
 */
@FunctionalInterface
public interface TransferFunctionInterceptorRegistry {

    /**
     * Registers the interceptor.
     */
    void registerHttpInterceptor(Interceptor interceptor);

}
