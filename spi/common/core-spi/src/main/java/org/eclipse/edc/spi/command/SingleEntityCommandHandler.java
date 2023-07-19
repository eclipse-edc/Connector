/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.command;

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.result.StoreFailure;

import static java.lang.String.format;

/**
 * Abstract handler for {@link SingleEntityCommand}s.
 */
public abstract class SingleEntityCommandHandler<C extends SingleEntityCommand, E extends StatefulEntity<E>> implements CommandHandler<C> {

    private final StateEntityStore<E> store;

    public SingleEntityCommandHandler(StateEntityStore<E> store) {
        this.store = store;
    }

    @Override
    public CommandResult handle(C command) {
        var entityId = command.getEntityId();
        var leaseResult = store.findByIdAndLease(entityId);
        if (leaseResult.failed()) {
            if (leaseResult.reason() == StoreFailure.Reason.NOT_FOUND) {
                return CommandResult.notFound(leaseResult.getFailureDetail());
            } else {
                return CommandResult.conflict(leaseResult.getFailureDetail());
            }
        }

        var entity = leaseResult.getContent();

        if (modify(entity, command)) {
            entity.setModified();
            store.save(entity);
            postActions(entity, command);
            return CommandResult.success();
        } else {
            store.save(entity);
            return CommandResult.conflict(format("Could not execute %s on %s with ID [%s] because it's %s",
                    command.getClass().getSimpleName(), entity.getClass().getSimpleName(), entityId,
                    entity.stateAsString()));
        }
    }

    /**
     * All operations (read/write/update) on the {@link StatefulEntity} should be done inside this method.
     * It will not get called if there was an error obtaining the entity from the store.
     * If the {@link StatefulEntity} was indeed modified, implementors should return {@code true}, otherwise {@code false}
     *
     * @param entity The {@link StatefulEntity}
     * @param command The {@link SingleEntityCommand}
     * @return true if the process was actually modified, false otherwise.
     */
    protected abstract boolean modify(E entity, C command);

    /**
     * Actions needed to be executed after persistance, only if the command modifies the entity.
     *
     * @param entity the entity.
     * @param command the command.
     */
    public void postActions(E entity, C command) {

    }
}
