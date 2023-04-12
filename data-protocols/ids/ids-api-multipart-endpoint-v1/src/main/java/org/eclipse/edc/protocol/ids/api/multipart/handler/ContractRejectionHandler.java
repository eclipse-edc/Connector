/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation, refactoring
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.ContractRejectionMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.MessageProtocol;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.processedFromServiceResult;

/**
 * This class handles and processes incoming IDS {@link ContractRejectionMessage}s.
 */
public class ContractRejectionHandler implements Handler {

    private final Monitor monitor;
    private final IdsId connectorId;
    private final ContractNegotiationService contractNegotiationService;

    public ContractRejectionHandler(Monitor monitor, IdsId connectorId, ContractNegotiationService contractNegotiationService) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.contractNegotiationService = contractNegotiationService;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        return multipartRequest.getHeader() instanceof ContractRejectionMessage;
    }

    @Override
    public @NotNull MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest) {
        var claimToken = multipartRequest.getClaimToken();
        var message = (ContractRejectionMessage) multipartRequest.getHeader();
        var correlationMessageId = message.getCorrelationMessage();
        var correlationId = message.getTransferContract();
        var rejectionReason = message.getContractRejectionReason();

        monitor.debug(String.format("ContractRejectionHandler: Received contract rejection to " +
                "message %s. Negotiation process: %s. Rejection Reason: %s", correlationMessageId,
                correlationId, rejectionReason));

        var terminationMessage = TransferTerminationMessage.Builder.newInstance()
                .processId(String.valueOf(correlationId))
                .protocol(MessageProtocol.IDS_MULTIPART)
                .connectorAddress("") // this will be used by DSP implementation
                .build();

        var result = contractNegotiationService.notifyTerminated(terminationMessage, claimToken);

        return createMultipartResponse(processedFromServiceResult(result, message, connectorId));
    }

}
