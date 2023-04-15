/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.transform.ContractAgreementTransformerOutput;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.MessageProtocol;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.badParameters;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.processedFromServiceResult;

/**
 * This class handles and processes incoming IDS {@link de.fraunhofer.iais.eis.ContractAgreementMessage}s.
 */
public class ContractAgreementHandler implements Handler {

    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final IdsId connectorId;
    private final IdsTransformerRegistry transformerRegistry;
    private final ContractNegotiationProtocolService service;

    public ContractAgreementHandler(
            Monitor monitor, IdsId connectorId, ObjectMapper objectMapper,
            IdsTransformerRegistry transformerRegistry, ContractNegotiationProtocolService service) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.objectMapper = objectMapper;
        this.transformerRegistry = transformerRegistry;
        this.service = service;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        return multipartRequest.getHeader() instanceof de.fraunhofer.iais.eis.ContractAgreementMessage;
    }

    @Override
    public @NotNull MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest) {
        var claimToken = multipartRequest.getClaimToken();
        var message = (de.fraunhofer.iais.eis.ContractAgreementMessage) multipartRequest.getHeader();

        ContractAgreement contractAgreement;
        try {
            contractAgreement = objectMapper.readValue(multipartRequest.getPayload(), ContractAgreement.class);
        } catch (IOException e) {
            monitor.severe("ContractAgreementHandler: Contract Agreement is invalid", e);
            return createMultipartResponse(badParameters(message, connectorId));
        }

        var permission = contractAgreement.getPermission().get(0);
        if (permission == null) {
            monitor.debug("ContractAgreementHandler: Contract Agreement is invalid");
            return createMultipartResponse(badParameters(message, connectorId));
        }

        var result = transformerRegistry.transform(contractAgreement, ContractAgreementTransformerOutput.class);
        if (result.failed()) {
            monitor.debug(String.format("Could not transform contract agreement: [%s]", result.getFailureDetail()));
            return createMultipartResponse(badParameters(message, connectorId));
        }

        var output = result.getContent();

        var request = ContractAgreementMessage.Builder.newInstance()
                .protocol(MessageProtocol.IDS_MULTIPART)
                .connectorId(String.valueOf(message.getIssuerConnector()))
                .connectorAddress("") // this will be used by DSP
                .contractAgreement(output.getContractAgreement())
                .processId(String.valueOf(message.getTransferContract()))
                .policy(output.getPolicy())
                .build();

        var negotiationConfirmResult = service.notifyAgreed(request, claimToken);

        return createMultipartResponse(processedFromServiceResult(negotiationConfirmResult, message, connectorId));
    }

}
