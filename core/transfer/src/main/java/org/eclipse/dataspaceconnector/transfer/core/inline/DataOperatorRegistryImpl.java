package org.eclipse.dataspaceconnector.transfer.core.inline;

import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataReader;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataStreamPublisher;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataWriter;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataOperatorRegistryImpl implements DataOperatorRegistry {

    private final List<DataStreamPublisher> copiers = new CopyOnWriteArrayList<>();
    private final List<DataReader> readers = new CopyOnWriteArrayList<>();
    private final List<DataWriter> writers = new CopyOnWriteArrayList<>();

    @Override
    public void registerStreamPublisher(DataStreamPublisher copier) {
        copiers.add(copier);
    }

    @Override
    public void registerReader(DataReader reader) {
        readers.add(reader);
    }

    @Override
    public void registerWriter(DataWriter writer) {
        writers.add(writer);
    }

    @Override
    public @Nullable DataStreamPublisher getStreamPublisher(DataRequest dataRequest) {
        return copiers.stream().filter(t -> t.canHandle(dataRequest)).findFirst().orElse(null);
    }

    @Override
    public DataReader getReader(String type) {
        return readers.stream().filter(t -> t.canHandle(type)).findFirst().orElse(null);
    }

    @Override
    public DataWriter getWriter(String type) {
        return writers.stream().filter(t -> t.canHandle(type)).findFirst().orElse(null);
    }
}
