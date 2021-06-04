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
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.QueryRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.microsoft.dagx.common.Cast.cast;

/**
 *
 */
@ExtendWith(DagxExtension.class)
@Disabled
public class ClientRunner {
    private static final String PROVIDER_CONNECTOR = "http://localhost:8181";
    private static final TokenResult US_TOKEN = TokenResult.Builder.newInstance().token("mock-us").build();
    private static final TokenResult EU_TOKEN = TokenResult.Builder.newInstance().token("mock-eu").build();
    private static final DataEntry<?> EU_ARTIFACT = DataEntry.Builder.newInstance().id("test123").build();
    private static final DataEntry<?> US_OR_EU_ARTIFACT = DataEntry.Builder.newInstance().id("test456").build();

    private CountDownLatch latch;

    @Test
    @Disabled
    void processClientRequest_toAws(RemoteMessageDispatcherRegistry dispatcherRegistry, TransferProcessManager processManager) throws Exception {

        var query = QueryRequest.Builder.newInstance()
                .connectorAddress(PROVIDER_CONNECTOR)
                .connectorId(PROVIDER_CONNECTOR)
                .queryLanguage("dagx")
                .query("select *")
                .protocol("ids-rest").build();

        CompletableFuture<List<String>> future = cast(dispatcherRegistry.send(List.class, query, () -> null));

        var artifacts = future.get();
        for (String artifact : artifacts) {
            // Initiate a request as a U.S.-based connector for an EU or US allowed artifact (will be accepted)
            var usOrEuRequest = createRequestAws("us-eu-request", DataEntry.Builder.newInstance().id(artifact).build());

            processManager.initiateClientRequest(usOrEuRequest);
        }

        // Initiate a request as a U.S.-based connector for an EU-restricted artifact (will be denied)
        var usRequest = createRequestAws("us-request", EU_ARTIFACT);

        processManager.initiateClientRequest(usRequest);


        latch.await(1, TimeUnit.DAYS);
    }


    @Test
//    @Disabled
    void processClientRequest_toAzureStorage(RemoteMessageDispatcherRegistry dispatcherRegistry, TransferProcessManager processManager) throws Exception {
        var query = QueryRequest.Builder.newInstance()
                .connectorAddress(PROVIDER_CONNECTOR)
                .connectorId(PROVIDER_CONNECTOR)
                .queryLanguage("dagx")
                .query("select *")
                .protocol("ids-rest").build();

        CompletableFuture<List<String>> future = cast(dispatcherRegistry.send(List.class, query, () -> null));

        var artifacts = future.get();
        for (String artifact : artifacts) {
            // Initiate a request as a U.S.-based connector for an EU or US allowed artifact (will be accepted)
            var usOrEuRequest = createRequestAzure("us-eu-request", DataEntry.Builder.newInstance().id(artifact).build());

            processManager.initiateClientRequest(usOrEuRequest);
        }

//        // Initiate a request as a U.S.-based connector for an EU-restricted artifact (will be denied)
//        var usRequest = createRequestAzure("us-request", EU_ARTIFACT);
//
//        processManager.initiateClientRequest(usRequest);

        latch.await(1, TimeUnit.DAYS);
    }

    @BeforeEach
    void before(DagxExtension extension) {
        IdentityService identityService = EasyMock.createMock(IdentityService.class);
        EasyMock.expect(identityService.obtainClientCredentials(EasyMock.isA(String.class))).andReturn(US_TOKEN).anyTimes();
        EasyMock.replay(identityService);
        latch = new CountDownLatch(1);

        extension.registerSystemExtension(ServiceExtension.class, TestExtensions.mockIamExtension(identityService));
    }

    private DataRequest createRequestAws(String id, DataEntry<?> artifactId) {
        return DataRequest.Builder.newInstance()
                .id(id)
                .protocol("ids-rest")
                .dataEntry(artifactId)
                .connectorId(PROVIDER_CONNECTOR)
                .connectorAddress(PROVIDER_CONNECTOR)
                .destinationType(S3BucketSchema.TYPE).build();
    }

    private DataRequest createRequestAzure(String id, DataEntry<?> artifactId) {
        return DataRequest.Builder.newInstance()
                .id(id)
                .protocol("ids-rest")
                .dataEntry(artifactId)
                .connectorId(PROVIDER_CONNECTOR)
                .connectorAddress(PROVIDER_CONNECTOR)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type(AzureBlobStoreSchema.TYPE)
                        .property(AzureBlobStoreSchema.ACCOUNT_NAME, "dagxblobstoreitest")
                        .property(AzureBlobStoreSchema.CONTAINER_NAME, "temp-dest-container")
                        .build())
                .destinationType(AzureBlobStoreSchema.TYPE)
                .build();
    }
}
