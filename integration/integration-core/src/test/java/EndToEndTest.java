import com.microsoft.dagx.junit.DagxExtension;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 */
@ExtendWith(DagxExtension.class)
public class EndToEndTest {

    @Test
    @Disabled
    void processRequest(TransferProcessManager processManager) {
        var artifactId = "test123";
        var connectorId = "https://test";

        DataEntry<?> entry = DataEntry.Builder.newInstance().id(artifactId).build();
        DataRequest request = DataRequest.Builder.newInstance().protocol("ids-rest").dataEntry(entry).connectorId(connectorId).connectorAddress(connectorId).destinationType("S3").build();

        processManager.initiateClientRequest(request);
        // TODO create mechanism to wait on a latch for async processing
        // Thread.sleep(10000);
    }
}
