package org.eclipse.dataspaceconnector.spi.iam;

import java.util.Collection;
import java.util.List;

public interface ObjectStore<T> {
    List<T> getAll(int limit);

    List<T> getAfter(String continuationToken);

    boolean save(T entity);

    T getLatest();

    void saveAll(Collection<T> entities);
}
