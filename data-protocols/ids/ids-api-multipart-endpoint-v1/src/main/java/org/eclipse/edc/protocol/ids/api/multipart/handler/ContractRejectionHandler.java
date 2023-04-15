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
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.MessageProtocol;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.processedFromServiceResult;

/**
 * This class handles and processes incoming IDS {@link ContractRejectionMessage}s.
 */
public class ContractRejectionHandler implements Handler {

    private final Monitor monitor;
    private final IdsId connectorId;
    private final ContractNegotiationProtocolService service;

    public ContractRejectionHandler(Monitor monitor, IdsId connectorId, ContractNegotiationProtocolService service) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.service = service;
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

        var rejectionMessage = ContractNegotiationTerminationMessage.Builder.newInstance()
                .processId(String.valueOf(correlationId))
                .protocol(MessageProtocol.IDS_MULTIPART)
                .connectorAddress("") // this will be used by DSP implementation
                .rejectionReason(Optional.ofNullable(rejectionReason).map(TypedLiteral::toString).orElse(""))
                .build();

        var result = service.notifyTerminated(rejectionMessage, claimToken);

        return createMultipartResponse(processedFromServiceResult(result, message, connectorId));
    }

}
