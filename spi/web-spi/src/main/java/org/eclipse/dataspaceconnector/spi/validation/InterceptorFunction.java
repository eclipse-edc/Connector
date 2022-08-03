/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.function.Function;

/**
 * Marker interface for functions that act as interceptors for method calls.
 * It will receive the method's arguments as {@code Object[]} and must return a {@code Result<Void>}.
 * <p>
 * A {@link InterceptorFunction} should not throw an exception as that may cause unexpected behaviour.
 */
@FunctionalInterface
public interface InterceptorFunction extends Function<Object[], Result<Void>> {
}
