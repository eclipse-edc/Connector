import com.microsoft.dagx.junit.DagxExtension;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@ExtendWith(DagxExtension.class)
public class EndToEndTest {

    @Test
    @Disabled
    void processRequest(TransferProcessManager processManager, DataFlowManager dataFlowManager) throws InterruptedException {
        DataFlowController dataFlowMock = EasyMock.createMock(DataFlowController.class);

        CountDownLatch latch = new CountDownLatch(1);

        EasyMock.expect(dataFlowMock.canHandle(EasyMock.isA(DataRequest.class))).andReturn(true);
        EasyMock.expect(dataFlowMock.initiateFlow(EasyMock.isA(DataRequest.class))).andAnswer(() -> {
            latch.countDown();
            return DataFlowInitiateResponse.OK;
        });
        EasyMock.replay(dataFlowMock);

        dataFlowManager.register(dataFlowMock);

        var artifactId = "test123";
        var connectorId = "https://test";

        DataEntry<?> entry = DataEntry.Builder.newInstance().id(artifactId).build();
        DataRequest request = DataRequest.Builder.newInstance().protocol("ids-rest").dataEntry(entry).connectorId(connectorId).connectorAddress(connectorId).destinationType("S3").build();

        processManager.initiateClientRequest(request);

        latch.await(4000, TimeUnit.DAYS);

        EasyMock.verify(dataFlowMock);
    }

    @BeforeEach
    void before(DagxExtension extension) {
        // register mocks needed for boot here
    }
}
