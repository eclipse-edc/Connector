import com.microsoft.dagx.junit.DagxExtension;
import com.microsoft.dagx.schema.aws.S3BucketSchema;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.iam.TokenResult;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
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
@Disabled
public class ClientRunner {
    private static final String PROVIDER_CONNECTOR = "http://localhost:8181";
    private static final TokenResult US_TOKEN = TokenResult.Builder.newInstance().token("mock-us").build();
    private static final TokenResult EU_TOKEN = TokenResult.Builder.newInstance().token("mock-eu").build();
    private static final DataEntry<?> EU_ARTIFACT = DataEntry.Builder.newInstance().id("test123").build();
    private static final DataEntry<?> US_OR_EU_ARTIFACT = DataEntry.Builder.newInstance().id("test456").build();

    private IdentityService identityService;

    @Test
    void processClientRequest(TransferProcessManager processManager) throws InterruptedException {

        EasyMock.expect(identityService.obtainClientCredentials(EasyMock.isA(String.class))).andReturn(US_TOKEN);
        EasyMock.expect(identityService.obtainClientCredentials(EasyMock.isA(String.class))).andReturn(EU_TOKEN);
        EasyMock.replay(identityService);

        CountDownLatch latch = new CountDownLatch(1);

        // Initiate a request as a U.S.-based connector for an EU-restricted artifact (will be denied)
        DataRequest usRequest = DataRequest.Builder.newInstance().id("us-request").protocol("ids-rest").dataEntry(EU_ARTIFACT).connectorId(PROVIDER_CONNECTOR).connectorAddress(PROVIDER_CONNECTOR).destinationType(S3BucketSchema.TYPE).build();

        processManager.initiateClientRequest(usRequest);

        // Initiate a request as a U.S.-based connector for an EU or US allowed artifact (will be accepted)
        DataRequest usOrEuRequest = DataRequest.Builder.newInstance().id("us-eu--request").protocol("ids-rest").dataEntry(US_OR_EU_ARTIFACT).connectorId(PROVIDER_CONNECTOR).connectorAddress(PROVIDER_CONNECTOR).destinationType(S3BucketSchema.TYPE).build();

        processManager.initiateClientRequest(usOrEuRequest);

        latch.await(1, TimeUnit.DAYS);
    }


    @BeforeEach
    void before(DagxExtension extension) {
        identityService = EasyMock.createMock(IdentityService.class);
        extension.registerSystemExtension(ServiceExtension.class, TestExtensions.mockIamExtension(identityService));
    }
}
