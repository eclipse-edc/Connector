package org.eclipse.dataspaceconnector.dataloading;

/**
 * Backing store for ingesting items.
 */
public interface DataSink<T> {
    void accept(T item);
}
