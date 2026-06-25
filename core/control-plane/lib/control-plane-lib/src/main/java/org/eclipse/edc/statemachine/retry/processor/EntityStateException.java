/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine.retry.processor;

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Exception that describes generic failure in a process
 */
public class EntityStateException extends RuntimeException {

    private final StatefulEntity<?> entity;
    private final String processName;

    public EntityStateException(StatefulEntity<?> entity, String processName, String message) {
        super(message);
        this.entity = entity;
        this.processName = processName;
    }

    public StatefulEntity<?> getEntity() {
        return entity;
    }

    public String getProcessName() {
        return processName;
    }

    @NotNull String getRetryLimitExceededMessage() {
        return "%s: ID %s. Attempt #%d failed to %s. Retry limit exceeded. Cause: %s".formatted(
                entity.getClass().getSimpleName(),
                entity.getId(),
                entity.getStateCount(),
                getProcessName(),
                getMessage()
        );
    }

    @NotNull String getRetryFailedMessage() {
        return "%s: ID %s. Attempt #%d failed to %s. Cause: %s".formatted(
                entity.getClass().getSimpleName(),
                entity.getId(),
                entity.getStateCount(),
                getProcessName(),
                getMessage()
        );
    }
}
