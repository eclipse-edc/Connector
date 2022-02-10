/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.inline.core;

import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataStreamPublisher;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

public class InlineDataFlowController implements DataFlowController {
    private final Vault vault;
    private final Monitor monitor;
    private final DataOperatorRegistry dataOperatorRegistry;
    private final DataAddressResolver dataAddressResolver;

    public InlineDataFlowController(Vault vault, Monitor monitor, DataOperatorRegistry dataOperatorRegistry, DataAddressResolver dataAddressResolver) {
        this.vault = vault;
        this.monitor = monitor;
        this.dataOperatorRegistry = dataOperatorRegistry;
        this.dataAddressResolver = dataAddressResolver;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        var sourceType = dataAddressResolver.resolveForAsset(dataRequest.getAssetId()).getType();
        var destinationType = dataRequest.getDestinationType();
        return dataOperatorRegistry.getStreamPublisher(dataRequest) != null ||
                (dataOperatorRegistry.getReader(sourceType) != null && dataOperatorRegistry.getWriter(destinationType) != null);
    }

    @Override
    public @NotNull DataFlowInitiateResult initiateFlow(DataRequest dataRequest) {
        var source = dataAddressResolver.resolveForAsset(dataRequest.getAssetId());
        var destinationType = dataRequest.getDestinationType();
        monitor.info(format("Copying data from %s to %s", source.getType(), destinationType));

        // first look for a streamer
        DataStreamPublisher streamer = dataOperatorRegistry.getStreamPublisher(dataRequest);
        if (streamer != null) {
            Result<Void> copyResult = streamer.notifyPublisher(dataRequest);
            if (copyResult.failed()) {
                return DataFlowInitiateResult.failure(ERROR_RETRY, "Failed to copy data from source to destination: " + copyResult.getFailure().getMessages());
            }
        } else {
            var destSecretName = dataRequest.getDataDestination().getKeyName();
            if (destSecretName == null) {
                monitor.severe(format("No credentials found for %s, will not copy!", destinationType));
                return DataFlowInitiateResult.failure(ERROR_RETRY, "Did not find credentials for data destination.");
            }

            var secret = vault.resolveSecret(destSecretName);
            // if no copier found for this source/destination pair, then use inline read and write
            var reader = dataOperatorRegistry.getReader(source.getType());
            var writer = dataOperatorRegistry.getWriter(destinationType);

            var readResult = reader.read(source);
            if (readResult.failed()) {
                return DataFlowInitiateResult.failure(ERROR_RETRY, "Failed to read data from source: " + readResult.getFailure().getMessages());
            }
            var writeResult = writer.write(dataRequest.getDataDestination(), dataRequest.getAssetId(), readResult.getContent(), secret);
            if (writeResult.failed()) {
                return DataFlowInitiateResult.failure(ERROR_RETRY, "Failed to write data to destination: " + writeResult.getFailure().getMessages());
            }
        }

        return DataFlowInitiateResult.success("Inline data flow successful");
    }
}
