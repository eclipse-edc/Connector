package com.microsoft.dagx.transfer.nifi;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntryPropertyLookup;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataEntryPropertyLookup;
import com.microsoft.dagx.spi.types.domain.transfer.DataDestination;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import com.microsoft.dagx.transfer.nifi.api.NifiApiClient;
import com.microsoft.dagx.transfer.types.azure.AzureStorageDestination;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class NifiDataFlowControllerTest {

    private final static String nifiHost = "http://localhost:8080";
    private final static String storageAccount = "dagxblobstoreitest";
    private static String storageAccountKey = null;

    private static String blobName;
    private static OkHttpClient httpClient;
    private static TypeManager typeManager;
    private static String containerName;
    private static NifiApiClient client;
    private static BlobContainerClient blobContainerClient;
    private NifiDataFlowController controller;
    private Vault vault;

    @BeforeAll
    public static void prepare() throws Exception {

        // this is necessary because the @EnabledIf... annotation does not prevent @BeforeAll to be called
        var isCi = propOrEnv("CI", "false");
        if (!Boolean.parseBoolean(isCi)) {
            return;
        }

        typeManager = new TypeManager();
        typeManager.getMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        httpClient = new OkHttpClient.Builder().build();

        var f = Thread.currentThread().getContextClassLoader().getResource("TwoClouds.xml");
        var file = new File(Objects.requireNonNull(f).toURI());
        client = new NifiApiClient(nifiHost, typeManager, httpClient);
        String processGroup = "root";
        try {
            var templateId = client.uploadTemplate(processGroup, file);
            client.instantiateTemplate(templateId);
        } catch (DagxException ignored) {
        } finally {
            var controllerService = client.getControllerServices(processGroup).get(0);
            var controllerServiceId = controllerService.id;
            var version = controllerService.revision.version;
            client.startControllerService(controllerServiceId, version);
            client.startProcessGroup(processGroup);
        }

        // create azure storage container
        containerName = "nifi-itest-" + UUID.randomUUID();

        storageAccountKey = propOrEnv("AZ_STORAGE_KEY", null);
        if (storageAccountKey == null) {
            throw new RuntimeException("No environment variable found AZ_STORAGE_KEY!");
        }

        try {
            var bsc = new BlobServiceClientBuilder().connectionString("DefaultEndpointsProtocol=https;AccountName=" + storageAccount + ";AccountKey=" + storageAccountKey + ";EndpointSuffix=core.windows.net")
                    .buildClient();
            blobContainerClient = bsc.createBlobContainer(containerName);
        } catch (BlobStorageException ex) {
            fail("Error initializing the Azure Blob Storage: ", ex);
        }
        // upload blob to storage
        blobName = "testimage.jpg";
        var blobClient = blobContainerClient.getBlobClient(blobName);
        URL testImageStream = Thread.currentThread().getContextClassLoader().getResource(blobName);
        String absolutePath = Objects.requireNonNull(Paths.get(testImageStream.toURI())).toString();
        blobClient.uploadFromFile(absolutePath, true);

    }

    @AfterAll
    public static void winddown() {
        blobContainerClient.delete();
    }

    @BeforeEach
    void setUp() {


        Monitor monitor = new Monitor() {
        };
        NifiTransferManagerConfiguration config = NifiTransferManagerConfiguration.Builder.newInstance().url("http://localhost:8888")
                .build();
        typeManager.registerTypes(DataRequest.class);

        vault = createMock(Vault.class);
        var nifiAuth = propOrEnv("NIFI_API_AUTH", null);
        if (nifiAuth == null) {
            throw new RuntimeException("No environment variable found NIFI_API_AUTH!");
        }
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(nifiAuth);
        replay(vault);
        controller = new NifiDataFlowController(config, typeManager, monitor, vault, httpClient);
    }

    @Test
    @Timeout(value = 10)
    void initiateFlow() throws InterruptedException {
        var ext = GenericDataEntryPropertyLookup.Builder.newInstance().property("type", "AzureStorage")
                .property("account", storageAccount)
                .property("container", containerName)
                .property("blobname", blobName)
                .property("key", storageAccountKey)
                .build();

        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryPropertyLookup> entry = DataEntry.Builder.newInstance().id(id).lookup(ext).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(AzureStorageDestination.Builder.newInstance()
                        .account(storageAccount)
                        .container(containerName)
                        .blobname("bike_very_new.jpg")
                        .key(storageAccountKey)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs().stream().noneMatch(blob -> blob.getName().equals("bike_very_new.jpg"))) {
            Thread.sleep(500);
        }

    }

    @Test
    @Timeout(10)
    void initiateFlow_sourceNotFound() throws InterruptedException {
        var bulletinSize = client.getBulletinBoard().bulletins.size();

        var ext = GenericDataEntryPropertyLookup.Builder.newInstance().property("type", "AzureStorage")
                .property("account", storageAccount)
                .property("container", containerName)
                .property("blobname", "notexist.png")
                .property("key", storageAccountKey)
                .build();

        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryPropertyLookup> entry = DataEntry.Builder.newInstance().id(id).lookup(ext).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(AzureStorageDestination.Builder.newInstance()
                        .account(storageAccount)
                        .container(containerName)
                        .blobname("will_not_succeed.jpg")
                        .key(storageAccountKey)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());

        // will fail the test if bulletinSize has not increased by 1 within 10 seconds
        while (bulletinSize + 1 != client.getBulletinBoard().bulletins.size()) {
            Thread.sleep(100);
        }

        assertEquals(bulletinSize + 1, client.getBulletinBoard().bulletins.size());
    }

    @Test
    void initiateFlow_noDestinationDefined() {
        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryPropertyLookup> entry = DataEntry.Builder.newInstance().lookup(GenericDataEntryPropertyLookup.Builder.newInstance().build()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .build();

        var response = controller.initiateFlow(dataRequest);
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(response.getError()).isEqualTo("Data target is null");

    }

    @Test
    void initiateFlow_noCredsFoundInVault() {
        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryPropertyLookup> entry = DataEntry.Builder.newInstance().lookup(GenericDataEntryPropertyLookup.Builder.newInstance().build()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataDestination(new DataDestination() {
                    @Override
                    public String getType() {
                        return "TestType";
                    }

                    @Override
                    public DestinationSecretToken getSecretToken() {
                        return null;
                    }
                })
                .dataEntry(entry)
                .build();

        reset(vault);
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(null);
        replay(vault);

        var response = controller.initiateFlow(dataRequest);
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(response.getError()).isEqualTo("NiFi vault credentials were not found");
    }

    private PagedIterable<BlobItem> listBlobs() {
        var bsc = new BlobServiceClientBuilder().connectionString("DefaultEndpointsProtocol=https;AccountName=" + storageAccount + ";AccountKey=" + storageAccountKey + ";EndpointSuffix=core.windows.net")
                .buildClient();
        var bcc = bsc.getBlobContainerClient(containerName);
        return bcc.listBlobs();
    }
}
