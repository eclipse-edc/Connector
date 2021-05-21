package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.transfer.demo.protocols.common.AbstractQueuedProvisioner;
import com.microsoft.dagx.transfer.demo.protocols.common.DataDestination;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.ConnectionResult;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.DestinationManager;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamObserver;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.Subscription;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.SubscriptionResult;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Implements simple, in-memory message destinations with pub/sub semantics.
 */
public class DemoDestinationManager extends AbstractQueuedProvisioner implements DestinationManager {
    private Monitor monitor;

    private Map<String, DestinationContainer> containerCache = new ConcurrentHashMap<>();
    private Map<String, StreamObserver> observers = new ConcurrentHashMap<>();

    public DemoDestinationManager(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public ConnectionResult connect(String destinationName, String accessToken) {
        var container = containerCache.get(destinationName);
        if (container == null) {
            return new ConnectionResult("Destination not found: " + destinationName);
        } else if (!container.destination.getAccessToken().equals(accessToken)) {
            return new ConnectionResult("Not authorized");
        }
        return new ConnectionResult(data -> {
            container.containers.values().forEach(c -> c.accept(data));
            observers.values().forEach(o -> o.onPublish(destinationName, data));
        });
    }

    @Override
    public SubscriptionResult subscribe(String destinationName, String accessToken, Consumer<byte[]> consumer) {
        Objects.requireNonNull(destinationName);
        var subscriptionId = UUID.randomUUID().toString();
        var container = getContainer(destinationName);
        if (!container.destination.getAccessToken().equals(accessToken)) {
            return new SubscriptionResult("Invalid key");
        }
        container.containers.put(subscriptionId, consumer);
        return new SubscriptionResult(new Subscription(destinationName, subscriptionId));
    }

    @Override
    public void unsubscribe(Subscription subscription) {
        getContainer(subscription.getDestinationName()).containers.remove(subscription.getSubscriptionId());
    }

    @Override
    public String registerObserver(StreamObserver observer) {
        var id = UUID.randomUUID().toString();
        observers.put(id, observer);
        return id;
    }

    @Override
    public void unRegisterObserver(String id) {
        observers.remove(id);
    }

    @Override
    protected void onEntry(QueueEntry entry) {
        var token = UUID.randomUUID().toString();
        var destination = new DataDestination(entry.getDestinationName(), token);
        var container = new DestinationContainer(destination);
        containerCache.put(entry.getDestinationName(), container);

        monitor.info("Provisioned a demo destination");

        entry.getFuture().complete(destination);

        containerCache.put(entry.getDestinationName(), new DestinationContainer(destination));
        entry.getFuture().complete(destination);
    }

    @NotNull
    private DestinationContainer getContainer(String destinationName) {
        var consumers = containerCache.get(destinationName);
        if (consumers == null) {
            throw new DagxException("Invalid destination: " + destinationName);
        }
        return consumers;
    }

    private static class DestinationContainer {
        DataDestination destination;
        Map<String, Consumer<byte[]>> containers = new ConcurrentHashMap<>();

        public DestinationContainer(DataDestination destination) {
            this.destination = destination;
        }
    }
}
