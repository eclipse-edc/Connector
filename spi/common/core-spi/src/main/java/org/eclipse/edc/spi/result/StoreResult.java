/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       ZF Friedrichshafen AG - added private property support
 *
 */

package org.eclipse.edc.spi.result;

import java.util.List;

import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_LEASED;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.DUPLICATE_KEYS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

/**
 * Specialized {@link Result} class to indicate the success or failure of an interaction with the persistence layer.
 *
 * @param <T> The type of content
 */
public class StoreResult<T> extends AbstractResult<T, StoreFailure, StoreResult<T>> {

    protected StoreResult(T content, StoreFailure failure) {
        super(content, failure);
    }

    public static <T> StoreResult<T> success(T content) {
        return new StoreResult<>(content, null);
    }

    public static <T> StoreResult<T> alreadyExists(String message) {
        return new StoreResult<>(null, new StoreFailure(List.of(message), ALREADY_EXISTS));
    }

    public static <T> StoreResult<T> notFound(String message) {
        return new StoreResult<>(null, new StoreFailure(List.of(message), NOT_FOUND));
    }

    public static <T> StoreResult<T> duplicateKeys(String message) {
        return new StoreResult<>(null, new StoreFailure(List.of(message), DUPLICATE_KEYS));
    }

    public static <T> StoreResult<T> success() {
        return StoreResult.success(null);
    }

    public static <T> StoreResult<T> alreadyLeased(String message) {
        return new StoreResult<>(null, new StoreFailure(List.of(message), ALREADY_LEASED));
    }

    public StoreFailure.Reason reason() {
        return getFailure().getReason();
    }
}
