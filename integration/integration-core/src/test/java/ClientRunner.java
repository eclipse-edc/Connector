/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

import com.microsoft.dagx.junit.DagxExtension;
import com.microsoft.dagx.schema.aws.S3BucketSchema;
import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.iam.TokenResult;
import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.transfer.TransferInitiateResponse;
import com.microsoft.dagx.spi.transfer.TransferProcessListener;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.TransferProcessObservable;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.QueryRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.easymock.EasyMock;
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

import static com.microsoft.dagx.common.Cast.cast;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DagxExtension.class)
@Disabled
public class ClientRunner {
    private static final String PROVIDER_CONNECTOR = "http://dev-connector.westeurope.cloudapp.azure.com/";
    private static final TokenResult US_TOKEN = TokenResult.Builder.newInstance().token("mock-us").build();
    private static final TokenResult EU_TOKEN = TokenResult.Builder.newInstance().token("mock-eu").build();
    private static final DataEntry EU_ARTIFACT = DataEntry.Builder.newInstance().id("test123").build();
    private static final DataEntry US_OR_EU_ARTIFACT = DataEntry.Builder.newInstance().id("test456").build();

    private CountDownLatch latch;

    @Test
    @Disabled
    void processClientRequest_toAws(RemoteMessageDispatcherRegistry dispatcherRegistry, TransferProcessManager processManager, TransferProcessObservable observable, TransferProcessStore store) throws Exception {

        var query = QueryRequest.Builder.newInstance()
                .connectorAddress(PROVIDER_CONNECTOR)
                .connectorId(PROVIDER_CONNECTOR)
                .queryLanguage("dagx")
                .query("select *")
                .protocol("ids-rest").build();

        CompletableFuture<List<String>> future = cast(dispatcherRegistry.send(List.class, query, () -> null));

        var artifacts = future.get();
        artifacts = artifacts.stream().findAny().stream().collect(Collectors.toList());
        latch = new CountDownLatch(artifacts.size());
        for (String artifact : artifacts) {
            System.out.println("processing artifact " + artifact);
            // Initiate a request as a U.S.-based connector for an EU or US allowed artifact (will be accepted)
            var usOrEuRequest = createRequestAws("us-eu-request-" + UUID.randomUUID(), DataEntry.Builder.newInstance().id(artifact).build());

            final TransferInitiateResponse response = processManager.initiateClientRequest(usOrEuRequest);
            observable.registerListener(response.getId(), new TransferProcessListener() {
                @Override
                public void completed(TransferProcess process) {
                    //simulate data egress
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    process.transitionDeprovisionRequested();
                    store.update(process);
                }

                @Override
                public void deprovisioned(TransferProcess process) {
                    latch.countDown();
                }
            });
        }

        // Initiate a request as a U.S.-based connector for an EU-restricted artifact (will be denied)
//        var usRequest = createRequestAws("us-request", EU_ARTIFACT);
//
//        processManager.initiateClientRequest(usRequest);


        assertThat(latch.await(5, TimeUnit.MINUTES)).isTrue();
    }


    @Test
    @Disabled
    void processClientRequest_toAzureStorage(RemoteMessageDispatcherRegistry dispatcherRegistry, TransferProcessManager processManager, TransferProcessObservable observable, TransferProcessStore store) throws Exception {
        var query = QueryRequest.Builder.newInstance()
                .connectorAddress(PROVIDER_CONNECTOR)
                .connectorId(PROVIDER_CONNECTOR)
                .queryLanguage("dagx")
                .query("select *")
                .protocol("ids-rest").build();

        CompletableFuture<List<String>> future = cast(dispatcherRegistry.send(List.class, query, () -> null));

        var artifacts = future.get();

        assertThat(artifacts).describedAs("Should have returned artifacts!").isNotEmpty();

        latch = new CountDownLatch(artifacts.size());

        for (String artifact : artifacts) {
            // Initiate a request as a U.S.-based connector for an EU or US allowed artifact (will be accepted)
            var usOrEuRequest = createRequestAzure("us-eu-request-" + UUID.randomUUID(), DataEntry.Builder.newInstance().id(artifact).build());

            final TransferInitiateResponse response = processManager.initiateClientRequest(usOrEuRequest);
            observable.registerListener(response.getId(), new TransferProcessListener() {
                @Override
                public void completed(TransferProcess process) {
                    //simulate data egress
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    process.transitionDeprovisionRequested();
                    store.update(process);
                }

                @Override
                public void deprovisioned(TransferProcess process) {
                    latch.countDown();
                }
            });
        }

        // Initiate a request as a U.S.-based connector for an EU-restricted artifact (will be denied)
//        var usRequest = createRequestAzure("us-request", EU_ARTIFACT);
//
//        processManager.initiateClientRequest(usRequest);

        assertThat(latch.await(5, TimeUnit.MINUTES)).isTrue();
    }

    @BeforeEach
    void before(DagxExtension extension) {
        IdentityService identityService = EasyMock.createMock(IdentityService.class);
        EasyMock.expect(identityService.obtainClientCredentials(EasyMock.isA(String.class))).andReturn(US_TOKEN).anyTimes();
        EasyMock.replay(identityService);
        latch = new CountDownLatch(1);

        extension.registerSystemExtension(ServiceExtension.class, TestExtensions.mockIamExtension(identityService));
    }

    private DataRequest createRequestAws(String id, DataEntry artifactId) {
        return DataRequest.Builder.newInstance()
                .id(id)
                .protocol("ids-rest")
                .dataEntry(artifactId)
                .connectorId(PROVIDER_CONNECTOR)
                .connectorAddress(PROVIDER_CONNECTOR)
                .destinationType(S3BucketSchema.TYPE).build();
    }

    private DataRequest createRequestAzure(String id, DataEntry artifactId) {
        return DataRequest.Builder.newInstance()
                .id(id)
                .protocol("ids-rest")
                .dataEntry(artifactId)
                .connectorId(PROVIDER_CONNECTOR)
                .connectorAddress(PROVIDER_CONNECTOR)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type(AzureBlobStoreSchema.TYPE)
                        .property(AzureBlobStoreSchema.ACCOUNT_NAME, "dagxtfblob")
                        .property(AzureBlobStoreSchema.CONTAINER_NAME, "temp-dest-container-" + UUID.randomUUID())
                        .build())
                .destinationType(AzureBlobStoreSchema.TYPE)
                .build();
    }
}
