import com.microsoft.dagx.junit.DagxExtension;
import com.microsoft.dagx.spi.message.RemoteMessageDispatcher;
import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
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

        EasyMock.expect(dispatcher.send(EasyMock.notNull(), EasyMock.isA(RemoteMessage.class))).andAnswer(() -> {
            var future = new CompletableFuture<>();
            future.complete(null);
            latch.countDown();
            return future;
        });

        EasyMock.replay(dispatcher);

        dispatcherRegistry.register(dispatcher);

        var artifactId = "test123";
        var connectorId = "https://test";

        DataEntry<?> entry = DataEntry.Builder.newInstance().id(artifactId).build();
        DataRequest request = DataRequest.Builder.newInstance().protocol("ids-rest").dataEntry(entry).connectorId(connectorId).connectorAddress(connectorId).destinationType("S3").build();

        processManager.initiateClientRequest(request);

        latch.await(4000, TimeUnit.DAYS);

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

        DataEntry<?> entry = DataEntry.Builder.newInstance().id(artifactId).build();
        DataRequest request = DataRequest.Builder.newInstance().protocol("ids-rest").dataEntry(entry).connectorId(connectorId).connectorAddress(connectorId).destinationType("S3").id(UUID.randomUUID().toString()).build();

        processManager.initiateProviderRequest(request);

        latch.await(4000, TimeUnit.DAYS);

        EasyMock.verify(controllerMock);
    }

    @BeforeEach
    void before(DagxExtension extension) {
        // register mocks needed for boot here
    }
}
