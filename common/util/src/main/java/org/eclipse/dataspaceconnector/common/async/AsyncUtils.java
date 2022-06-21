/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.common.async;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class AsyncUtils {

    private AsyncUtils() {
    }

    public static <X, T extends CompletableFuture<X>> Collector<T, ?, CompletableFuture<List<X>>> asyncAllOf() {
        Function<List<T>, CompletableFuture<List<X>>> finisher = list -> CompletableFuture
                .allOf(list.toArray(CompletableFuture[]::new))
                .thenApply(v -> list.stream().map(CompletableFuture::join).collect(Collectors.toList()));

        return Collectors.collectingAndThen(Collectors.toList(), finisher);
    }
}
