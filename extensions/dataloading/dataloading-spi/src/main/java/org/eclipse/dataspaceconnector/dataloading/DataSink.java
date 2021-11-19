package org.eclipse.dataspaceconnector.dataloading;

public interface DataSink<T> {
    void accept(T item);
}
