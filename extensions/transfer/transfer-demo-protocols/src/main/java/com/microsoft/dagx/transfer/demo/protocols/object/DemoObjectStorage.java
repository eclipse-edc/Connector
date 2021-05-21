package com.microsoft.dagx.transfer.demo.protocols.object;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.transfer.demo.protocols.common.AbstractQueuedProvisioner;
import com.microsoft.dagx.transfer.demo.protocols.common.DataDestination;
import com.microsoft.dagx.transfer.demo.protocols.spi.object.ObjectStorage;
import com.microsoft.dagx.transfer.demo.protocols.spi.object.ObjectStorageObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class DemoObjectStorage extends AbstractQueuedProvisioner implements ObjectStorage {
    private Monitor monitor;

    private Map<String, ObjectContainer> containers = new ConcurrentHashMap<>();
    private List<ObjectStorageObserver> observers = new ArrayList<>();

    public DemoObjectStorage(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void register(ObjectStorageObserver observer) {
        observers.add(observer);
    }

    @Override
    public boolean store(String containerName, String objectKey, String token, byte[] data) {
        Objects.requireNonNull(containerName);
        Objects.requireNonNull(objectKey);
        Objects.requireNonNull(token);
        Objects.requireNonNull(data);
        var container = containers.get(containerName);
        if (container == null) {
            monitor.severe("Object container not found: " + containerName);
            return false;
        }
        if (!container.credentials.getAccessToken().equals(token)) {
            monitor.severe("Invalid token for container: " + containerName);
            return false;
        }
        container.objects.put(objectKey, data);

        monitor.severe("Stored data object to container: " + containerName);

        observers.forEach(o -> o.onStore(containerName, objectKey, token, data));

        return true;
    }

    @Override
    public boolean deprovision(String containerName) {
        containers.remove(containerName);
        monitor.severe("De-provisioned object container: " + containerName);

        observers.forEach(o -> o.onDeprovision(containerName));
        return true;
    }

    @Override
    protected void onEntry(AbstractQueuedProvisioner.QueueEntry entry) {
        var token = UUID.randomUUID().toString();
        var destination = new DataDestination(entry.getDestinationName(), token);
        var container = new ObjectContainer(destination);
        containers.put(entry.getDestinationName(), container);
        monitor.info("Provisioned a demo object container");
        observers.forEach(o -> o.onProvision(destination));
        entry.getFuture().complete(destination);
    }

    private static class ObjectContainer {
        DataDestination credentials;

        Map<String, byte[]> objects = new ConcurrentHashMap<>();

        public ObjectContainer(DataDestination credentials) {
            this.credentials = credentials;
        }
    }

    private static class QueueEntry {
        CompletableFuture<DataDestination> future;

        public QueueEntry(CompletableFuture<DataDestination> future) {
            this.future = future;
        }
    }
}
