/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.transfer.spi.testfixtures.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.UUID;

public class TestFunctions {

    public static ResourceManifest createManifest() {
        return ResourceManifest.Builder.newInstance()
                .build();
    }

    public static DataRequest createDataRequest() {
        return createDataRequest("test-process-id");
    }

    public static DataRequest.Builder createDataRequestBuilder() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataDestination(createDataAddressBuilder("Test Address Type")
                        .keyName("Test Key Name")
                        .build())
                .connectorAddress("http://some-connector.com")
                .protocol("ids-multipart")
                .connectorId("some-connector")
                .contractId("some-contract")
                .managedResources(false)
                .assetId(Asset.Builder.newInstance().id("asset-id").build().getId())
                .claimToken(ClaimToken.Builder.newInstance().claim("claim", "value").build())
                .processId("test-process-id");
    }

    public static DataRequest createDataRequest(String transferProcessId) {
        return createDataRequestBuilder().processId(transferProcessId)
                .build();
    }

    public static TransferProcess createTransferProcess() {
        return createTransferProcess("test-process");
    }

    public static TransferProcess createTransferProcess(String processId, TransferProcessStates state) {
        return createTransferProcessBuilder(processId).state(state.code()).build();
    }

    public static TransferProcess createTransferProcess(String processId, DataRequest dataRequest) {
        return createTransferProcessBuilder(processId).dataRequest(dataRequest).build();
    }

    public static TransferProcess createTransferProcess(String processId) {
        return createTransferProcessBuilder(processId)
                .state(TransferProcessStates.UNSAVED.code())
                .build();
    }

    public static TransferProcess.Builder createTransferProcessBuilder(String processId) {
        return TransferProcess.Builder.newInstance()
                .id(processId)
                .createdAt(Clock.systemUTC().millis())
                .state(TransferProcessStates.UNSAVED.code())
                .type(TransferProcess.Type.CONSUMER)
                .dataRequest(createDataRequest())
                .contentDataAddress(createDataAddressBuilder("any").build())
                .resourceManifest(createManifest());
    }

    public static DataAddress.Builder createDataAddressBuilder(String type) {
        return DataAddress.Builder.newInstance()
                .type(type);
    }

    @NotNull
    public static TransferProcess initialTransferProcess() {
        return initialTransferProcess(UUID.randomUUID().toString(), "clientid");
    }

    @NotNull
    public static TransferProcess initialTransferProcess(String processId, String dataRequestId) {
        var process = createTransferProcess(processId, createDataRequestBuilder().id(dataRequestId).build());
        process.transitionInitial();
        return process;
    }

    @JsonTypeName("dataspaceconnector:testresourcedef")
    @JsonDeserialize(builder = TestResourceDef.Builder.class)
    public static class TestResourceDef extends ResourceDefinition {

        @Override
        public <RD extends ResourceDefinition, B extends ResourceDefinition.Builder<RD, B>> B toBuilder() {
            return null;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ResourceDefinition.Builder<TestResourceDef, Builder> {
            private Builder() {
                super(new TestResourceDef());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder();
            }
        }
    }

    @JsonDeserialize(builder = TestProvisionedResource.Builder.class)
    @JsonTypeName("dataspaceconnector:testprovisionedresource")
    public static class TestProvisionedResource extends ProvisionedResource {

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ProvisionedResource.Builder<TestProvisionedResource, Builder> {
            private Builder() {
                super(new TestProvisionedResource());
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder();
            }
        }
    }
}
