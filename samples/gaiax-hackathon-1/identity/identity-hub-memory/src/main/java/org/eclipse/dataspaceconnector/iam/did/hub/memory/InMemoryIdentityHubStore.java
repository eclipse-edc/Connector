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
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.hub.memory;

import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.Commit;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.HubObject;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.stream.Collectors.toList;

/**
 * An im-memory implementation of an {@link IdentityHubStore}.
 *
 * This implementation is ephemeral and not intended for production use.
 */
public class InMemoryIdentityHubStore implements IdentityHubStore {
    private Map<String, List<Commit>>commitIdCache = new HashMap<>(); // commits stored by initial (create commit) object id
    private Map<String, Map<String, List<HubObject>>> hubCache = new HashMap<>();

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void write(Commit commit) {
        lock.writeLock().lock();
        try {
           switch (commit.getOperation()) {
               case create:
                   commitIdCache.computeIfAbsent(commit.getObjectId(), k-> new ArrayList<>()).add(commit);
                   var qualifiedType = commit.getQualifiedType();
                   hubCache.computeIfAbsent(qualifiedType, k->new HashMap<>()).computeIfAbsent(commit.getObjectId(), k-> new ArrayList<>()).add(createObject(commit));
                   break;
               case update:
                   break;
               case delete:
                   break;
           }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Collection<Commit> query(CommitQuery query) {
        lock.readLock().lock();
        try {
            var commits = commitIdCache.get(query.getObjectId());
            return commits == null ? Collections.emptyList() : commits;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<HubObject> query(ObjectQuery query) {
        lock.readLock().lock();
        switch (query.getInterface()) {
            case Collections:
                try {
                    var objects = hubCache.get(query.getQualifiedType());
                    if (objects == null) {
                        return Collections.emptyList();
                    }

                    return objects.values().stream().flatMap(Collection::stream).collect(toList());
                } finally {
                    lock.readLock().unlock();
                }
            case Actions:
            case Permissions:
            case Profile:
                throw new UnsupportedOperationException("Not implemented");
        }
        return null;
    }

    private HubObject createObject(Commit commit) {
      return HubObject.Builder.newInstance()
              .createdBy(commit.getIss())
              .id(commit.getObjectId())
              .type(commit.getType())
              .commitStrategy(commit.getCommitStrategy())
              .interfaze(commit.getInterface())
              .sub(commit.getSub())
              .build();
    }

}
