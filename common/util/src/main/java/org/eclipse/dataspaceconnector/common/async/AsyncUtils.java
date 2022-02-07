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
