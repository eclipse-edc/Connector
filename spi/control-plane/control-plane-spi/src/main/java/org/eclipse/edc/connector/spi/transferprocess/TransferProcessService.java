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
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
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
     * Notifies the TransferProcess that it has been STARTED by the counter part.
     * Only callable on CONSUMER TransferProcess
     *
     * @param dataRequestId the dataRequestId
     * @return a succeeded result if the operation was successful, a failed one otherwise
     */
    ServiceResult<TransferProcess> started(String dataRequestId);

    /**
     * Asynchronously requests cancellation of the transfer process.
     * <p>
     * The return result status only reflects the successful submission of the command.
     *
     * @param transferProcessId id of the transferProcess
     * @return a result that is successful if the transfer process was found and is in a state that can be canceled
     * @deprecated use {@link #terminate} instead
     */
    @Deprecated(since = "milestone9")
    @NotNull
    default ServiceResult<TransferProcess> cancel(String transferProcessId) {
        return terminate(transferProcessId, "transfer cancelled");
    }

    /**
     * Asynchronously requests termination of the transfer process.
     * <p>
     * The return result status only reflects the successful submission of the command.
     *
     * @param transferProcessId id of the transferProcess
     * @param reason reason for the termination
     * @return a result that is successful if the transfer process was found and is in a state that can be terminated
     */
    @NotNull
    ServiceResult<TransferProcess> terminate(String transferProcessId, String reason);

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


    /**
     * Asynchronously informs the system that the {@link DeprovisionedResource} has been provisioned
     *
     * @param transferProcessId The transfer process id
     * @param resource The {@link DeprovisionedResource} to deprovision
     * @return a result that is successful if the transfer process was found
     */
    ServiceResult<TransferProcess> completeDeprovision(String transferProcessId, DeprovisionedResource resource);

    /**
     * Asynchronously handles a {@link ProvisionResponse} received from an external system
     *
     * @param transferProcessId The transfer process id
     * @param response The {@link ProvisionResponse} to handle
     * @return a result that is successful if the transfer process was found
     */
    ServiceResult<TransferProcess> addProvisionedResource(String transferProcessId, ProvisionResponse response);

}
