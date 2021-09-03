package org.eclipse.dataspaceconnector.samples.identity.did;

import org.eclipse.dataspaceconnector.ion.model.did.resolution.DidDocument;
import org.eclipse.dataspaceconnector.ion.spi.DidStore;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryDidDocumentStore implements DidStore {

    private final List<Entity<DidDocument>> memoryDb;

    public InMemoryDidDocumentStore() {
        memoryDb = new CopyOnWriteArrayList<>();
    }

    @Override
    public List<DidDocument> getAll(int limit) {
        return memoryDb.stream().sorted().limit(limit).map(e -> e.payload).collect(Collectors.toList());
    }

    @Override
    public List<DidDocument> getAfter(String continuationToken) {
        if (memoryDb.stream().noneMatch(e -> e.getPayload().getId().equals(continuationToken))) {
            return Collections.emptyList();
        }

        return memoryDb.stream()
                .dropWhile(e -> !e.getPayload().getId().equals(continuationToken))
                .map(Entity::getPayload)
                .collect(Collectors.toList());
    }

    @Override
    public boolean save(DidDocument entity) {
        if (memoryDb.stream().noneMatch(e -> e.getPayload().getId().equals(entity.getId()))) {
            memoryDb.add(new Entity<>(entity));
            return true;
        }
        return false;
    }

    @Override
    public DidDocument getLatest() {
        if (memoryDb.isEmpty()) {
            return null;
        }

        return memoryDb.get(memoryDb.size() - 1).getPayload();
    }

    @Override
    public void saveAll(Collection<DidDocument> entities) {
        // to transaction handling is required here
        entities.forEach(this::save);
    }

    @Override
    @Nullable
    public DidDocument forId(String did) {
        // TODO this can be much more efficient
        var result = memoryDb.stream().filter(d -> did.equals(d.payload.getId())).findFirst();
        return result.map(didDocumentEntity -> didDocumentEntity.payload).orElse(null);
    }

    private static class Entity<T> implements Comparable<Entity<T>> {
        private final Instant createTime = Instant.now();
        private final T payload;

        public Entity(T payload) {
            this.payload = payload;
        }

        public T getPayload() {
            return payload;
        }

        public Instant getCreateTime() {
            return createTime;
        }

        @Override
        public int compareTo(Entity<T> other) {
            return createTime.compareTo(other.getCreateTime());
        }
    }
}
