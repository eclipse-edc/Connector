package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;

import java.util.function.Predicate;

public interface PredicateFactory<T> {
    Predicate<T> convert(Criterion criterion);
}
