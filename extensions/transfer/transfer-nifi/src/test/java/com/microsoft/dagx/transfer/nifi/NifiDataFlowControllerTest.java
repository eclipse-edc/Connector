/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.microsoft.dagx.catalog.atlas.dataseed.AzureBlobFileEntityBuilder;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasApiImpl;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasDataCatalog;
import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.schema.SchemaRegistryImpl;
import com.microsoft.dagx.schema.azure.AzureBlobStoreSchema;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataCatalog;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataCatalog;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.transfer.nifi.api.NifiApiClient;
import okhttp3.OkHttpClient;
import org.apache.atlas.AtlasClientV2;
import org.easymock.MockType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
public class NifiDataFlowControllerTest {

    private static final String ATLAS_API_HOST = "http://localhost:21000";
    private static final String NIFI_CONTENTLISTENER_HOST = "http://localhost:8888";
    private final static String NIFI_API_HOST = "http://localhost:8080";
    private final static String storageAccount = "dagxblobstoreitest";
    private final static String atlasUsername = "admin";
    private final static String atlasPassword = "admin";
    private static String sharedAccessSignature = null;
    private static String blobName;
    private static OkHttpClient httpClient;
    private static TypeManager typeManager;
    private static String containerName;
    private static BlobContainerClient blobContainerClient;
    private NifiDataFlowController controller;
    private Vault vault;


    @BeforeAll
    public static void prepare() throws Exception {

//         this is necessary because the @EnabledIf... annotation does not prevent @BeforeAll to be called
        var isCi = propOrEnv("CI", "false");
        if (!Boolean.parseBoolean(isCi)) {
            return;
        }

        typeManager = new TypeManager();
        typeManager.getMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        httpClient = new OkHttpClient.Builder().build();

        var f = Thread.currentThread().getContextClassLoader().getResource("TwoClouds.xml");
        var file = new File(Objects.requireNonNull(f).toURI());
        NifiApiClient client = new NifiApiClient(NIFI_API_HOST, typeManager, httpClient);
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

        sharedAccessSignature = propOrEnv("AZ_STORAGE_SAS", null);
        if (sharedAccessSignature == null) {
            throw new RuntimeException("No environment variable found AZ_STORAGE_SAS!");
        }

        try {
            var connectionString = "BlobEndpoint=https://" + storageAccount + ".blob.core.windows.net/;SharedAccessSignature=" + sharedAccessSignature;
            var bsc = new BlobServiceClientBuilder().connectionString(connectionString)
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
        NifiTransferManagerConfiguration config = NifiTransferManagerConfiguration.Builder.newInstance().url(NIFI_CONTENTLISTENER_HOST)
                .build();
        typeManager.registerTypes(DataRequest.class);

        vault = mock(MockType.STRICT, Vault.class);
        var nifiAuth = propOrEnv("NIFI_API_AUTH", null);
        if (nifiAuth == null) {
            throw new RuntimeException("No environment variable found NIFI_API_AUTH!");
        }
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(nifiAuth);
        expect(vault.resolveSecret(storageAccount + "-key1")).andReturn(sharedAccessSignature);
        expect(vault.resolveSecret(storageAccount + "-key1")).andReturn(sharedAccessSignature);
        replay(vault);
        SchemaRegistry registry = new SchemaRegistryImpl();
        registry.register(new AzureBlobStoreSchema());
        controller = new NifiDataFlowController(config, typeManager, monitor, vault, httpClient, new NifiTransferEndpointConverter(registry, vault));
    }

    @Test
    @Timeout(value = 60)
    void initiateFlow_withAtlasCatalog() throws InterruptedException {

        // create custom atlas type and an instance
        String id;
        var schema = new AzureBlobStoreSchema();
        AtlasApi atlasApi = new AtlasApiImpl(new AtlasClientV2(new String[]{ATLAS_API_HOST}, new String[]{atlasUsername, atlasPassword}));
        try {

            atlasApi.createCustomTypes(schema.getName(), Set.of("DataSet"), new ArrayList<>(schema.getAttributes()));
        } catch (Exception ignored) {
        }
        id = atlasApi.createEntity(schema.getName(), AzureBlobFileEntityBuilder.newInstance()
                .withDescription("This is a test description")
                .withAccount(storageAccount)
                .withContainer(containerName)
                .withBlobname(blobName)
                .withKeyName(storageAccount + "-key1")
                .build());

        // perform the actual source file properties in Apache Atlas
        var lookup = new AtlasDataCatalog(atlasApi);
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().id(id).catalog(lookup).build();

        // connect the "source" (i.e. the lookup) and the "destination"
        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("account", storageAccount)
                        .property("container", containerName)
                        .property("blobname", "bike_very_new.jpg")
                        .keyName(storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());

        atlasApi.deleteEntities(Collections.singletonList(id));

        // will fail if new blob is not there after 60 seconds
        while (listBlobs().stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs().stream().anyMatch(bi -> bi.getName().equals("bike_very_new.jpg"))).isTrue();

    }

    @Test
    @Timeout(value = 10)
    void initiateFlow_withInMemCatalog() throws InterruptedException {

        String id = UUID.randomUUID().toString();
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().id(id).catalog(createLookup()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("account", storageAccount)
                        .property("container", containerName)
                        .property("blobname", "bike_very_new.jpg")
                        .keyName(storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs().stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs().stream().anyMatch(bi -> bi.getName().equals("bike_very_new.jpg"))).isTrue();

    }

    @Test
    void initiateFlow_sourceNotFound() {
        String id = UUID.randomUUID().toString();
        GenericDataCatalog lookup = createLookup();
        lookup.getProperties().replace("blobname", "notexist.png");
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance()
                .id(id)
                .catalog(lookup)
                .build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("account", storageAccount)
                        .property("container", containerName)
                        .property("blobname", "will_not_succeed.jpg")
                        .keyName(storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());

    }


    @Test
    void initiateFlow_noCredsFoundInVault() {
        String id = UUID.randomUUID().toString();
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().catalog(createLookup()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataDestination(DataAddress.Builder.newInstance().type("TestType")
                        .keyName(storageAccount + "-key1").build())
                .dataEntry(entry)
                .build();

        reset(vault);
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(null);
        replay(vault);

        var response = controller.initiateFlow(dataRequest);
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(response.getError()).isEqualTo("NiFi vault credentials were not found");
    }


    private GenericDataCatalog createLookup() {
        return GenericDataCatalog.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("account", storageAccount)
                .property("container", containerName)
                .property("blobname", blobName)
                .property("keyName", storageAccount + "-key1")
                .build();

    }

    private PagedIterable<BlobItem> listBlobs() {
        var connectionString = "BlobEndpoint=https://" + storageAccount + ".blob.core.windows.net/;SharedAccessSignature=" + sharedAccessSignature;
        var bsc = new BlobServiceClientBuilder().connectionString(connectionString)
                .buildClient();
        var bcc = bsc.getBlobContainerClient(containerName);
        return bcc.listBlobs();
    }
}
