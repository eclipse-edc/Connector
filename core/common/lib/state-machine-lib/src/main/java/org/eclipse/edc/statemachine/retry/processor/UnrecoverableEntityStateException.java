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
 * Exception that describes an unrecoverable failure in process.
 */
public class UnrecoverableEntityStateException extends EntityStateException {
    public UnrecoverableEntityStateException(StatefulEntity<?> entity, String processName, String message) {
        super(entity, processName, message);
    }

    @NotNull String getUnrecoverableMessage() {
        return "%s: ID %s. Attempt #%d failed to %s. Fatal error occurred. Cause: %s".formatted(
                getEntity().getClass().getSimpleName(),
                getEntity().getId(),
                getEntity().getStateCount(),
                getProcessName(),
                getMessage()
        );
    }
}
