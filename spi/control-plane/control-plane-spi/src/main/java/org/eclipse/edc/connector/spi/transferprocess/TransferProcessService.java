/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - initiate provider process
 *
 */

package org.eclipse.edc.connector.spi.transferprocess;

import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Mediates access to and modification of {@link TransferProcess}es.
 */
public interface TransferProcessService {

    /**
     * Returns a transferProcess by its id.
     *
     * @param transferProcessId id of the transferProcess
     * @return the transferProcess, null if it's not found
     */
    @Nullable
    TransferProcess findById(String transferProcessId);

    /**
     * Query transferProcess.
     *
     * @param query request
     * @return the collection of transferProcesses that match the query
     */
    ServiceResult<Stream<TransferProcess>> query(QuerySpec query);

    /**
     * Returns the state of a transferProcess by its id.
     *
     * @param transferProcessId id of the transferProcess
     * @return the transferProcess state name, null if it's not found
     */
    @Nullable
    String getState(String transferProcessId);

    /**
     * Asynchronously requests cancellation of the transfer process.
     * <p>
     * The return result status only reflects the successful submission of the command.
     *
     * @param transferProcessId id of the transferProcess
     * @return a result that is successful if the transfer process was found and is in a state that can be canceled
     */
    @NotNull
    ServiceResult<TransferProcess> cancel(String transferProcessId);


    /**
     * Asynchronously requests completion of the transfer process.
     * <p>
     * The return result status only reflects the successful submission of the command.
     *
     * @param transferProcessId id of the transferProcess
     * @return a result that is successful if the transfer process was found and is in a state that can be completed
     */
    @NotNull
    ServiceResult<TransferProcess> complete(String transferProcessId);

    /**
     * Asynchronously requests failure of the transfer process.
     * <p>
     * The return result status only reflects the successful submission of the command.
     *
     * @param transferProcessId id of the transferProcess
     * @param errorDetail the reason of the failure
     * @return a result that is successful if the transfer process was found and is in a state that can be failed
     */
    @NotNull
    ServiceResult<TransferProcess> fail(String transferProcessId, String errorDetail);

    /**
     * Asynchronously requests deprovisioning of the transfer process.
     * <p>
     * The return result status only reflects the successful submission of the command.
     *
     * @param transferProcessId id of the transferProcess
     * @return a result that is successful if the transfer process was found and is in a state that can be
     *         deprovisioned
     */
    @NotNull
    ServiceResult<TransferProcess> deprovision(String transferProcessId);

    /**
     * Initiate transfer request for type consumer.
     *
     * @param request for the transfer.
     * @return a result that is successful if the transfer process was initiated with id of created transferProcess.
     */
    @NotNull
    ServiceResult<String> initiateTransfer(DataRequest request);
    
    /**
     * Initiate transfer request for type provider.
     *
     * @param request for the transfer.
     * @param claimToken of the requesting participant.
     * @return a result that is successful if the transfer process was initiated with id of created transferProcess.
     */
    @NotNull
    ServiceResult<String> initiateTransfer(DataRequest request, ClaimToken claimToken);
}
