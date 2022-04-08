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

package org.eclipse.dataspaceconnector.transfer.core.inline;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataStreamPublisher;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

@Deprecated
public class InlineDataFlowController implements DataFlowController {
    private final Vault vault;
    private final Monitor monitor;
    private final DataOperatorRegistry dataOperatorRegistry;

    public InlineDataFlowController(Vault vault, Monitor monitor, DataOperatorRegistry dataOperatorRegistry) {
        this.vault = vault;
        this.monitor = monitor;
        this.dataOperatorRegistry = dataOperatorRegistry;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        var sourceType = contentAddress.getType();
        var destinationType = dataRequest.getDestinationType();
        return dataOperatorRegistry.getStreamPublisher(dataRequest) != null ||
                (dataOperatorRegistry.getReader(sourceType) != null && dataOperatorRegistry.getWriter(destinationType) != null);
    }

    @Override
    public @NotNull StatusResult<String> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        var destinationType = dataRequest.getDestinationType();
        monitor.info(format("Copying data from %s to %s", contentAddress.getType(), destinationType));

        // first look for a streamer
        DataStreamPublisher streamer = dataOperatorRegistry.getStreamPublisher(dataRequest);
        if (streamer != null) {
            Result<Void> copyResult = streamer.notifyPublisher(dataRequest);
            if (copyResult.failed()) {
                return StatusResult.failure(ERROR_RETRY, "Failed to copy data from source to destination: " + copyResult.getFailure().getMessages());
            }
        } else {
            var destSecretName = dataRequest.getDataDestination().getKeyName();
            if (destSecretName == null) {
                monitor.severe(format("No credentials found for %s, will not copy!", destinationType));
                return StatusResult.failure(ERROR_RETRY, "Did not find credentials for data destination.");
            }

            var secret = vault.resolveSecret(destSecretName);
            // if no copier found for this source/destination pair, then use inline read and write
            var reader = dataOperatorRegistry.getReader(contentAddress.getType());
            var writer = dataOperatorRegistry.getWriter(destinationType);

            var readResult = reader.read(contentAddress);
            if (readResult.failed()) {
                return StatusResult.failure(ERROR_RETRY, "Failed to read data from source: " + readResult.getFailure().getMessages());
            }
            var writeResult = writer.write(dataRequest.getDataDestination(), dataRequest.getAssetId(), readResult.getContent(), secret);
            if (writeResult.failed()) {
                return StatusResult.failure(ERROR_RETRY, "Failed to write data to destination: " + writeResult.getFailure().getMessages());
            }
        }

        return StatusResult.success("Inline data flow successful");
    }
}
