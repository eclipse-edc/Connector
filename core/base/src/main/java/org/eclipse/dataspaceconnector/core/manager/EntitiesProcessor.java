package org.eclipse.dataspaceconnector.core.manager;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class EntitiesProcessor<T> {

    private final Supplier<Collection<T>> entities;
    private final Predicate<Boolean> isProcessed = it -> it;

    public EntitiesProcessor(Supplier<Collection<T>> entitiesSupplier) {
        this.entities = entitiesSupplier;
    }

    public long doProcess(Function<T, Boolean> process) {
        return entities.get().stream()
                .map(process)
                .filter(isProcessed)
                .count();
    }
}
