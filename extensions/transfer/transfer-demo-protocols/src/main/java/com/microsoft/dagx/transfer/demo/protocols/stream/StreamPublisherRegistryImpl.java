package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamPublisher;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class StreamPublisherRegistryImpl implements StreamPublisherRegistry {
    private Vault vault;
    private ObjectMapper objectMapper;
    private Monitor monitor;

    private List<StreamPublisher> publishers = new ArrayList<>();

    public StreamPublisherRegistryImpl(Vault vault, ObjectMapper objectMapper, Monitor monitor) {
        this.vault = vault;
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    @Override
    public void register(StreamPublisher publisher) {
        var context = new PushStreamContext(vault, objectMapper, monitor);
        publisher.initialize(context);
        publishers.add(publisher);
    }

    @Override
    public void notifyPublisher(DataRequest data) {
        for (StreamPublisher publisher : publishers) {
            if (publisher.canHandle(data)) {
                publisher.notifyPublisher(data);
                return;
            }
        }
        throw new DagxException("No stream publisher found for request: " + data.getId());
    }
}
