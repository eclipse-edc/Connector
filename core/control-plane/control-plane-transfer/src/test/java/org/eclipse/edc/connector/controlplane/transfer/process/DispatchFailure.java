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

package org.eclipse.edc.connector.controlplane.transfer.process;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.params.provider.Arguments;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;


public class DispatchFailure implements Arguments {

    private final TransferProcessStates starting;
    private final TransferProcessStates ending;
    private final CompletableFuture<StatusResult<Object>> result;
    private final UnaryOperator<TransferProcess.Builder> builderEnricher;

    public DispatchFailure() {
        this(INITIAL, INITIAL, CompletableFuture.failedFuture(new RuntimeException("any")), it -> it);
    }

    public DispatchFailure(TransferProcessStates starting, TransferProcessStates ending, CompletableFuture<StatusResult<Object>> result, UnaryOperator<TransferProcess.Builder> builderEnricher) {
        this.starting = starting;
        this.ending = ending;
        this.result = result;
        this.builderEnricher = builderEnricher;
    }

    @Override
    public Object[] get() {
        return new Object[]{ starting, ending, result, builderEnricher };
    }
}
