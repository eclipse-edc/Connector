package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.CacheQuery;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.stream.Stream;

public interface QueryAdapter {
    Stream<Asset> executeQuery(CacheQuery query);

    boolean canExecute(CacheQuery query);
}
