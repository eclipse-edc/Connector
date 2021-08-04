package org.eclipse.dataspaceconnector.junit;/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

import org.eclipse.dataspaceconnector.common.testfixtures.EdcExtension;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
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
@ExtendWith(EdcExtension.class)
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
    void before(EdcExtension extension) {
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
