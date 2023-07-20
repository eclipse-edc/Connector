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

package org.eclipse.edc.spi.entity;

import java.util.function.Predicate;

/**
 * Provides a way to control the flow of an entity in the state machine.
 * If the function returns true, the entity will be marked as "pending" and it won't be picked up
 * again by the state machine.
 *
 * @param <E> the entity type
 */
@FunctionalInterface
public interface PendingGuard<E extends StatefulEntity<E>> extends Predicate<E> {
}
