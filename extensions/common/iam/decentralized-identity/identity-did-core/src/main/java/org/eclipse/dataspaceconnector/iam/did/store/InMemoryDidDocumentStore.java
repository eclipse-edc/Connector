/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.store;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryDidDocumentStore implements DidStore {

    private final Clock clock;
    private final List<Entity<DidDocument>> memoryDb;

    public InMemoryDidDocumentStore(Clock clock) {
        this.clock = clock;
        this.memoryDb = new CopyOnWriteArrayList<>();
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
    public void saveAll(Collection<DidDocument> entities) {
        // to transaction handling is required here
        entities.forEach(this::save);
    }

    @Override
    public DidDocument getLatest() {
        if (memoryDb.isEmpty()) {
            return null;
        }

        return memoryDb.get(memoryDb.size() - 1).getPayload();
    }

    @Override
    @Nullable
    public DidDocument forId(String did) {
        // TODO this can be much more efficient
        var result = memoryDb.stream().filter(d -> did.equals(d.payload.getId())).findFirst();
        return result.map(didDocumentEntity -> didDocumentEntity.payload).orElse(null);
    }

    private class Entity<T> implements Comparable<Entity<T>> {
        private final Instant createTime = clock.instant();
        private final T payload;

        Entity(T payload) {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entity<?> entity = (Entity<?>) o;
            return createTime.equals(entity.createTime) && payload.equals(entity.payload);
        }

        @Override
        public int hashCode() {
            return Objects.hash(createTime, payload);
        }
    }
}
