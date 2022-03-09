/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.common.statemachine;

/**
 * Interface that declares an abstraction for a component that process some entities and return the number of the processed ones.
 * Used by {@link StateMachine} to decide whether to apply wait strategy in loop iteration
 *
 */
@FunctionalInterface
public interface StateProcessor {

    /**
     * Process states
     *
     * @return the processed states count
     */
    Long process();
}
