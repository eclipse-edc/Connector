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

package org.eclipse.dataspaceconnector.junit;

import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.QueryRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.types.Cast.cast;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
@Disabled
public class ConsumerRunner {
    private static final String PROVIDER_CONNECTOR = "http://localhost:8181/";
    private static final TokenRepresentation US_TOKEN = TokenRepresentation.Builder.newInstance().token("mock-us").build();
    private static final TokenRepresentation EU_TOKEN = TokenRepresentation.Builder.newInstance().token("mock-eu").build();

    private CountDownLatch latch;

    @Test
    @Disabled
    void processConsumerRequest_toAws(RemoteMessageDispatcherRegistry dispatcherRegistry, TransferProcessManager processManager, TransferProcessObservable observable, TransferProcessStore store) throws Exception {

        var query = QueryRequest.Builder.newInstance()
                .connectorAddress(PROVIDER_CONNECTOR)
                .connectorId(PROVIDER_CONNECTOR)
                .queryLanguage("dataspaceconnector")
                .query("select *")
                .protocol(Protocols.IDS_MULTIPART).build();

        CompletableFuture<List<String>> future = cast(dispatcherRegistry.send(List.class, query, () -> null));

        var artifacts = future.get();
        artifacts = artifacts.stream().findAny().stream().collect(Collectors.toList());
        latch = new CountDownLatch(artifacts.size());
        for (String artifact : artifacts) {
            System.out.println("processing artifact " + artifact);
            // Initiate a request as a U.S.-based connector for an EU or US allowed artifact (will be accepted)
            var usOrEuRequest = createRequestAws("us-eu-request-" + UUID.randomUUID(), Asset.Builder.newInstance().id(artifact).build());

            var response = processManager.initiateConsumerRequest(usOrEuRequest);
            observable.registerListener(new TransferProcessListener() {
                @Override
                public void preCompleted(TransferProcess process) {
                    if (process.getId().equals(response.getContent())) {
                        return;
                    }
                    //simulate data egress
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    process.transitionDeprovisioning();
                    store.update(process);
                }

                @Override
                public void preDeprovisioned(TransferProcess process) {
                    if (process.getId().equals(response.getContent())) {
                        return;
                    }
                    latch.countDown();
                }
            });
        }

        // Initiate a request as a U.S.-based connector for an EU-restricted artifact (will be denied)
        // var usRequest = createRequestAws("us-request", EU_ARTIFACT);
        // processManager.initiateConsumerRequest(usRequest);


        assertThat(latch.await(5, TimeUnit.MINUTES)).isTrue();
    }

    @Test
    @Disabled
    void copyFromAzureBlobToAws(RemoteMessageDispatcherRegistry dispatcherRegistry, TransferProcessManager processManager, TransferProcessObservable observable, TransferProcessStore store) throws Exception {

        var query = QueryRequest.Builder.newInstance()
                .connectorAddress(PROVIDER_CONNECTOR)
                .connectorId(PROVIDER_CONNECTOR)
                .queryLanguage("dataspaceconnector")
                .query("select *")
                .protocol(Protocols.IDS_MULTIPART).build();

        CompletableFuture<List<String>> future = cast(dispatcherRegistry.send(List.class, query, () -> null));

        var artifacts = future.get();
        artifacts = artifacts.stream().findAny().stream().collect(Collectors.toList());
        latch = new CountDownLatch(artifacts.size());
        for (String artifact : artifacts) {
            System.out.println("processing artifact " + artifact);
            // Initiate a request as a U.S.-based connector for an EU or US allowed artifact (will be accepted)
            var usOrEuRequest = createRequestAws("us-eu-request-" + UUID.randomUUID(), Asset.Builder.newInstance().id(artifact).build());

            var response = processManager.initiateConsumerRequest(usOrEuRequest);
            observable.registerListener(new TransferProcessListener() {
                @Override
                public void preCompleted(TransferProcess process) {
                    if (process.getId().equals(response.getContent())) {
                        return;
                    }
                    //simulate data egress
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    process.transitionDeprovisioning();
                    store.update(process);
                }

                @Override
                public void preDeprovisioned(TransferProcess process) {
                    if (!process.getId().equals(response.getContent())) {
                        return;
                    }
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(5, TimeUnit.MINUTES)).isTrue();
    }


    @Test
    @Disabled
    void processClientRequest_toAzureStorage(RemoteMessageDispatcherRegistry dispatcherRegistry, TransferProcessManager processManager, TransferProcessObservable observable, TransferProcessStore store) throws Exception {
        var query = QueryRequest.Builder.newInstance()
                .connectorAddress(PROVIDER_CONNECTOR)
                .connectorId(PROVIDER_CONNECTOR)
                .queryLanguage("dataspaceconnector")
                .query("select *")
                .protocol(Protocols.IDS_MULTIPART).build();

        CompletableFuture<List<String>> future = cast(dispatcherRegistry.send(List.class, query, () -> null));

        var artifacts = future.get();

        assertThat(artifacts).describedAs("Should have returned artifacts!").isNotEmpty();

        latch = new CountDownLatch(artifacts.size());

        for (String artifact : artifacts) {
            // Initiate a request as a U.S.-based connector for an EU or US allowed artifact (will be accepted)
            var usOrEuRequest = createRequestAzure("us-eu-request-" + UUID.randomUUID(), Asset.Builder.newInstance().id(artifact).build());

            var response = processManager.initiateConsumerRequest(usOrEuRequest);
            observable.registerListener(new TransferProcessListener() {
                @Override
                public void preCompleted(TransferProcess process) {
                    if (process.getId().equals(response.getContent())) {
                        return;
                    }
                    //simulate data egress
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    process.transitionDeprovisioning();
                    store.update(process);
                }

                @Override
                public void preDeprovisioned(TransferProcess process) {
                    if (process.getId().equals(response.getContent())) {
                        return;
                    }
                    latch.countDown();
                }
            });
        }

        // Initiate a request as a U.S.-based connector for an EU-restricted artifact (will be denied)
        //        var usRequest = createRequestAzure("us-request", EU_ARTIFACT);
        //
        //        processManager.initiateConsumerRequest(usRequest);

        assertThat(latch.await(5, TimeUnit.MINUTES)).isTrue();
    }

    @BeforeEach
    void before(EdcExtension extension) {
        IdentityService identityService = mock(IdentityService.class);
        when(identityService.obtainClientCredentials(isA(String.class))).thenReturn(Result.success(US_TOKEN));

        latch = new CountDownLatch(1);

        extension.registerSystemExtension(ServiceExtension.class, TestExtensions.mockIamExtension(identityService));
    }

    private DataRequest createRequestAws(String id, Asset asset) {
        return DataRequest.Builder.newInstance()
                .id(id)
                .protocol(Protocols.IDS_MULTIPART)
                .assetId(asset.getId())
                .connectorId(PROVIDER_CONNECTOR)
                .connectorAddress(PROVIDER_CONNECTOR)
                .destinationType(S3BucketSchema.TYPE).build();
    }

    private DataRequest createRequestAzure(String id, Asset asset) {
        return DataRequest.Builder.newInstance()
                .id(id)
                .protocol(Protocols.IDS_MULTIPART)
                .assetId(asset.getId())
                .connectorId(PROVIDER_CONNECTOR)
                .connectorAddress(PROVIDER_CONNECTOR)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("type")
                        .property("account", "edcdemogpstorage")
                        .property("container", "temp-dest-container-" + UUID.randomUUID())
                        .build())
                .destinationType("type")
                .build();
    }
}
