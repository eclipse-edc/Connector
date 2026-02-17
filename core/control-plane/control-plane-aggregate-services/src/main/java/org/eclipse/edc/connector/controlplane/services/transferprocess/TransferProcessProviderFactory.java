/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.transferprocess;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.telemetry.Telemetry;

import java.time.Clock;

import static java.util.UUID.randomUUID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;

/**
 * A factory class for creating {@link TransferProcess} instances for providers.
 * This class encapsulates the dependencies required to construct and initialize a transfer process.
 */
public class TransferProcessProviderFactory {

    private final Clock clock;
    private final Telemetry telemetry;
    private final AssetIndex assetIndex;

    public TransferProcessProviderFactory(Clock clock, Telemetry telemetry, AssetIndex assetIndex) {
        this.clock = clock;
        this.telemetry = telemetry;
        this.assetIndex = assetIndex;
    }

    /**
     * Creates a new instance of {@link TransferProcess} for a provider based on the given inputs.
     * This method validates the asset associated with the provided {@link ContractAgreement}
     * and constructs a transfer process, encapsulating all required information.
     *
     * @param participantContext the context of the participant initiating the transfer process
     * @param message the transfer request message containing details of the request
     * @param contractAgreement the contract agreement associated with the transfer process
     * @return a {@link ServiceResult} containing the successfully created {@link TransferProcess},
     *         or an error message if the asset could not be found
     */
    public ServiceResult<TransferProcess> create(ParticipantContext participantContext, TransferRequestMessage message, ContractAgreement contractAgreement) {
        var asset = assetIndex.findById(contractAgreement.getAssetId());
        if (asset == null) {
            return ServiceResult.badRequest("Asset " + contractAgreement.getAssetId() + " not found");
        }

        var process = TransferProcess.Builder.newInstance()
                .id(randomUUID().toString())
                .protocol(message.getProtocol())
                .correlationId(message.getConsumerPid())
                .counterPartyAddress(message.getCallbackAddress())
                .assetId(contractAgreement.getAssetId())
                .contractId(contractAgreement.getId())
                .transferType(message.getTransferType())
                .type(PROVIDER)
                .clock(clock)
                .traceContext(telemetry.getCurrentTraceContext())
                .participantContextId(participantContext.getParticipantContextId())
                .dataplaneMetadata(asset.getDataplaneMetadata())
                .build();

        return ServiceResult.success(process);
    }
}
