package com.microsoft.dagx.transfer.nifi;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.transfer.nifi.api.NifiApiClient;
import org.junit.jupiter.api.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

class NifiDataFlowControllerTest {

    private final static String nifiHost = "http://localhost:8080";
    private final static String storageAccount = "nififlowtest";
    private static String storageAccountKey = null;

    private static String blobName;

    private NifiDataFlowController controller;
    private Vault vault;
    private static TypeManager typeManager;
    private static String containerName;
    private static NifiApiClient client;
    private static BlobContainerClient blobContainerClient;

    @BeforeAll
    public static void prepare() throws Exception {

        //todo: spin up dockerized nifi
        typeManager = new TypeManager();
        typeManager.getMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var f = Thread.currentThread().getContextClassLoader().getResource("TwoClouds.xml");
        var file = new File(Objects.requireNonNull(f).toURI());
        client = new NifiApiClient(nifiHost, typeManager);
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

        var bsc = new BlobServiceClientBuilder().connectionString("DefaultEndpointsProtocol=https;AccountName=nififlowtest;AccountKey=" + storageAccountKey + ";EndpointSuffix=core.windows.net")
                .buildClient();
        blobContainerClient = bsc.createBlobContainer(containerName);

        // upload blob to storage

        blobName = "testimage.jpg";
        var blobClient = blobContainerClient.getBlobClient(blobName);
        URL testImageStream = Thread.currentThread().getContextClassLoader().getResource(blobName);
        String absolutePath = Objects.requireNonNull(Paths.get(testImageStream.toURI())).toString();
        blobClient.uploadFromFile(absolutePath, true);

    }

    @BeforeEach
    void setUp() {


        Monitor monitor = new Monitor() {
        };
        NifiTransferManagerConfiguration config = NifiTransferManagerConfiguration.Builder.newInstance().url("http://localhost:8888")
                .build();
        typeManager.registerTypes(DataRequest.class);

        vault = createMock(Vault.class);
        var nifiAuth= propOrEnv("NIFI_API_AUTH", null);
        if(nifiAuth == null){
            throw new RuntimeException("No environment variable found NIFI_API_AUTH!");
        }
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(nifiAuth);
        replay(vault);
        controller = new NifiDataFlowController(config, typeManager, monitor, vault);
    }

    @Test
    @Timeout(value = 10)
    void initiateFlow() throws InterruptedException {
        var ext = GenericDataEntryExtensions.Builder.newInstance().property("type", "AzureStorage")
                .property("account", storageAccount)
                .property("container", containerName)
                .property("blobname", blobName)
                .property("key", storageAccountKey)
                .build();

        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().id(id).extensions(ext).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataTarget(AzureStorageTarget.Builder.newInstance()
                        .account(storageAccount)
                        .container(containerName)
                        .blobName("bike_very_new.jpg")
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

        var ext = GenericDataEntryExtensions.Builder.newInstance().property("type", "AzureStorage")
                .property("account", storageAccount)
                .property("container", containerName)
                .property("blobname", "notexist.png")
                .property("key", storageAccountKey)
                .build();

        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().id(id).extensions(ext).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataTarget(AzureStorageTarget.Builder.newInstance()
                        .account(storageAccount)
                        .container(containerName)
                        .blobName("will_not_succeed.jpg")
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
        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().extensions(GenericDataEntryExtensions.Builder.newInstance().build()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .build();

        var e = assertThrows(DagxException.class, () -> controller.initiateFlow(dataRequest));
        assertEquals(IllegalArgumentException.class, e.getCause().getClass());
    }

    @Test
    void initiateFlow_noCredsFoundInVault() {
        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().extensions(GenericDataEntryExtensions.Builder.newInstance().build()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataTarget(() -> "TestType")
                .dataEntry(entry)
                .build();

        reset(vault);
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(null);
        replay(vault);

        assertThrows(DagxException.class, () -> controller.initiateFlow(dataRequest), "No NiFi credentials found in Vault!");
    }

    @AfterAll
    public static void winddown() {
        blobContainerClient.delete();
    }

    private PagedIterable<BlobItem> listBlobs() {
        var bsc = new BlobServiceClientBuilder().connectionString("DefaultEndpointsProtocol=https;AccountName=nififlowtest;AccountKey=" + storageAccountKey + ";EndpointSuffix=core.windows.net")
                .buildClient();
        var bcc = bsc.getBlobContainerClient(containerName);
        return bcc.listBlobs();
    }
}
