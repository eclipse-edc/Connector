/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

import com.microsoft.dagx.junit.DagxExtension;
import com.microsoft.dagx.spi.iam.ClaimToken;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.iam.TokenResult;
import com.microsoft.dagx.spi.iam.VerificationResult;
import com.microsoft.dagx.spi.message.MessageContext;
import com.microsoft.dagx.spi.message.RemoteMessageDispatcher;
import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.types.domain.message.RemoteMessage;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@ExtendWith(DagxExtension.class)
public class EndToEndTest {

    @Test
    @Disabled
    void processClientRequest(TransferProcessManager processManager, RemoteMessageDispatcherRegistry dispatcherRegistry) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        RemoteMessageDispatcher dispatcher = EasyMock.createMock(RemoteMessageDispatcher.class);

        dispatcher.protocol();
        EasyMock.expectLastCall().andReturn("ids-rest");

        EasyMock.expect(dispatcher.send(EasyMock.notNull(), EasyMock.isA(RemoteMessage.class), EasyMock.isA(MessageContext.class))).andAnswer(() -> {
            var future = new CompletableFuture<>();
            future.complete(null);
            latch.countDown();
            return future;
        });

        EasyMock.replay(dispatcher);

        dispatcherRegistry.register(dispatcher);

        var artifactId = "test123";
        var connectorId = "https://test";

        DataEntry entry = DataEntry.Builder.newInstance().id(artifactId).build();
        DataRequest request = DataRequest.Builder.newInstance().protocol("ids-rest").dataEntry(entry).connectorId(connectorId).connectorAddress(connectorId).destinationType("S3").build();

        processManager.initiateClientRequest(request);

        latch.await(1, TimeUnit.MINUTES);

        EasyMock.verify(dispatcher);
    }

    @Test
    @Disabled
    void processProviderRequest(TransferProcessManager processManager, DataFlowManager dataFlowManager) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        DataFlowController controllerMock = EasyMock.createMock(DataFlowController.class);

        EasyMock.expect(controllerMock.canHandle(EasyMock.isA(DataRequest.class))).andReturn(true);
        EasyMock.expect(controllerMock.initiateFlow(EasyMock.isA(DataRequest.class))).andAnswer(() -> {
            latch.countDown();
            return DataFlowInitiateResponse.OK;
        });

        EasyMock.replay(controllerMock);

        dataFlowManager.register(controllerMock);


        var artifactId = "test123";
        var connectorId = "https://test";

        DataEntry entry = DataEntry.Builder.newInstance().id(artifactId).build();
        DataRequest request = DataRequest.Builder.newInstance().protocol("ids-rest").dataEntry(entry).connectorId(connectorId).connectorAddress(connectorId).destinationType("S3").id(UUID.randomUUID().toString()).build();

        processManager.initiateProviderRequest(request);

        latch.await(1, TimeUnit.MINUTES);

        EasyMock.verify(controllerMock);
    }

    @BeforeEach
    void before(DagxExtension extension) {
        extension.registerSystemExtension(ServiceExtension.class, new ServiceExtension() {
            @Override
            public Set<String> provides() {
                return Set.of("iam");
            }

            @Override
            public void initialize(ServiceExtensionContext context) {
                context.registerService(IdentityService.class, new IdentityService() {
                    @Override
                    public TokenResult obtainClientCredentials(String scope) {
                        return TokenResult.Builder.newInstance().token("test").build();
                    }

                    @Override
                    public VerificationResult verifyJwtToken(String token, String audience) {
                        return new VerificationResult((ClaimToken) null);
                    }
                });
            }
        });
    }
}
