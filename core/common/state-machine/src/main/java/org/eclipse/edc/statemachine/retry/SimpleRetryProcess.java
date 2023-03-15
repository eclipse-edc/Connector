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

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.entity.StatefulEntity;

import java.util.function.Supplier;

/**
 * Provides retry capabilities to a synchronous process without a return type
 */
public class SimpleRetryProcess<E extends StatefulEntity<E>> extends RetryProcess<E, SimpleRetryProcess<E>> {

    private final Supplier<Boolean> process;

    public SimpleRetryProcess(E entity, Supplier<Boolean> process, SendRetryManager sendRetryManager) {
        super(entity, sendRetryManager);
        this.process = process;
    }

    @Override
    boolean process(E entity, String description) {
        return process.get();
    }

}
