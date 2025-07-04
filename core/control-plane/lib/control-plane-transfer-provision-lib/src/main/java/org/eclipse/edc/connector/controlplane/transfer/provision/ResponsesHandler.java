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

package org.eclipse.edc.connector.controlplane.transfer.provision;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Handles service invocation responses, wrapping all the logic needed.
 * This component apply changes to the TransferProcess object without persisting it.
 */
public interface ResponsesHandler<R> {

    /**
     * Handle TransferProcess service invocation responses
     *
     * @param transferProcess the transfer process interested in the response.
     * @param responses the responses.
     * @return true if the responses have been handled, false otherwise.
     */
    boolean handle(TransferProcess transferProcess, List<R> responses);

    /**
     * Actions intended to be executed after the persistence.
     *
     * @param transferProcess the TransferProcess
     */
    void postActions(TransferProcess transferProcess);

    @NotNull
    default Result<Void> toFatalError(StatusResult<?> result) {
        if (result.fatalError()) {
            return Result.failure(result.getFailureMessages());
        } else {
            return Result.success();
        }
    }
}
