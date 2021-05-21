/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.microsoft.dagx.catalog.atlas.dataseed.AzureBlobFileEntityBuilder;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasApi;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasApiImpl;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasDataCatalog;
import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.schema.SchemaRegistryImpl;
import com.microsoft.dagx.schema.aws.S3BucketSchema;
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
import com.microsoft.dagx.transfer.provision.azure.AzureSasToken;
import okhttp3.OkHttpClient;
import org.apache.atlas.AtlasClientV2;
import org.easymock.MockType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.microsoft.dagx.spi.util.ConfigurationFunctions.propOrEnv;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
public class NifiDataFlowControllerTest {

    private static final String DEFAULT_NIFI_HOST = "http://localhost";
    private final static String storageAccount = "dagxblobstoreitest";
    private final static String atlasUsername = "admin";
    private final static String atlasPassword = "admin";
    private static String atlasApiUrl;
    private static String nifiContentlistenerHost;
    private static String s3BucketName = "dagx-itest";
    //todo: move this to an env var or repo secret
    private static String s3AccessKeyId;
    private static String s3SecretAccessKey;
    private static String sharedAccessSignature = null;
    private static String blobName;
    private static OkHttpClient httpClient;
    private static TypeManager typeManager;
    private static String containerName;
    private static BlobContainerClient blobContainerClient;
    private static S3Client s3client;
    private NifiDataFlowController controller;
    private Vault vault;

    @BeforeAll
    public static void prepare() throws Exception {

        System.out.println("prepare");

//         this is necessary because the @EnabledIf... annotation does not prevent @BeforeAll to be called
        var isCi = propOrEnv("CI", "false");
        if (!Boolean.parseBoolean(isCi)) {
            return;
        }

        var host = propOrEnv("NIFI_URL", DEFAULT_NIFI_HOST);

        String nifiApiHost = host + ":8080";
        nifiContentlistenerHost = host + ":8888";
        atlasApiUrl = propOrEnv("ATLAS_URL", "http://localhost:21000");

        s3AccessKeyId = propOrEnv("S3_ACCESS_KEY_ID", null);
        if (s3AccessKeyId == null) {
            throw new RuntimeException("S3_ACCESS_KEY_ID cannot be null!");
        }
        s3SecretAccessKey = propOrEnv("S3_SECRET_ACCESS_KEY", null);
        if (s3SecretAccessKey == null) {
            throw new RuntimeException("S3_SECRET_ACCESS_KEY cannot be null!");
        }

        typeManager = new TypeManager();
        typeManager.getMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        httpClient = new OkHttpClient.Builder().build();

        var f = Thread.currentThread().getContextClassLoader().getResource("TwoClouds.xml");
        var file = new File(Objects.requireNonNull(f).toURI());
        NifiApiClient client = new NifiApiClient(nifiApiHost, typeManager, httpClient);
        String processGroup = "root";

        try {
            System.out.println("prepare - uploading template to nifi");
            var templateId = client.uploadTemplate(processGroup, file);
            System.out.println("prepare - instantiate template in nifi");
            client.instantiateTemplate(templateId);
            System.out.println("prepare - setup variable registry");
            var pg = client.getProcessGroup(processGroup);
            try {
                client.updateVariableRegistry(processGroup, Map.of("s3.accessKeyId", s3AccessKeyId, "s3.secretAccessKey", s3SecretAccessKey), pg.revision);
            } catch (Exception ex) {
                System.out.println("prepare - creating variable registry failed: " + ex.getMessage());
            }
        } catch (DagxException ignored) {
        } finally {
            System.out.println("prepare - start controller service");
            var controllerService = client.getControllerServices(processGroup).get(0);
            var version = controllerService.revision.version;
            var controllerServiceId = controllerService.id;
            client.startControllerService(controllerServiceId, version);
            System.out.println("prepare - start process group");
            client.startProcessGroup(processGroup);

        }

        blobName = "testimage.jpg";
        URL testImageStream = Thread.currentThread().getContextClassLoader().getResource(blobName);
        String absolutePath = Objects.requireNonNull(Paths.get(testImageStream.toURI())).toString();

        //prepare bucket, i.e, upload test file to bucket
        System.out.println("prepare - create S3 bucket");

        s3BucketName += UUID.randomUUID().toString();
        s3client = S3Client.builder().region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return s3AccessKeyId;
                    }

                    @Override
                    public String secretAccessKey() {
                        return s3SecretAccessKey;
                    }
                })).build();
        s3client.createBucket(CreateBucketRequest.builder().bucket(s3BucketName).build());

        System.out.println("prepare - upload image to S3 bucket");

        Path of = Path.of(testImageStream.toURI());
        RequestBody requestBody = RequestBody.fromFile(of);
        PutObjectResponse putObjectResponse = s3client.putObject(PutObjectRequest.builder().bucket(s3BucketName).key(blobName).build(), requestBody);
        String s = putObjectResponse.eTag();


        // create azure storage container
        containerName = "dagx-itest-" + UUID.randomUUID().toString();

        sharedAccessSignature = propOrEnv("AZ_STORAGE_SAS", null);
        if (sharedAccessSignature == null) {
            throw new RuntimeException("No environment variable found AZ_STORAGE_SAS!");
        }

        System.out.println("prepare - construct blobservice client");

        var bsc = new BlobServiceClientBuilder().sasToken(sharedAccessSignature)
                .endpoint("https://" + storageAccount + ".blob.core.windows.net")
                .buildClient();

        System.out.println("prepare - construct blob container client client");
        blobContainerClient = bsc.getBlobContainerClient(containerName);
        System.out.println("prepare - create container " + containerName);
        blobContainerClient.create();


        // upload blob to storage
        System.out.println("prepare - upload test file to blob store");

        var blobClient = blobContainerClient.getBlobClient(blobName);
        blobClient.uploadFromFile(absolutePath, true);

        System.out.println("prepare - done");

    }

    @AfterAll
    public static void winddown() {
        System.out.println("winddown - clean blobs");

        blobContainerClient.delete();

        System.out.println("winddown - clean bucket");
        var objects = listS3BucketContents();
        for (var obj : objects) {
            System.out.println("  Delete Bucket object " + obj.key());
            s3client.deleteObject(DeleteObjectRequest.builder().bucket(s3BucketName).key(obj.key()).build());
        }
        System.out.println("winddown - delete bucket");
        s3client.deleteBucket(DeleteBucketRequest.builder().bucket(s3BucketName).build());
    }

    private static List<S3Object> listS3BucketContents() {
        return s3client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(s3BucketName)
                .build())
                .contents();
    }

    @BeforeEach
    void setUp() {

        System.out.println("");

        Monitor monitor = new Monitor() {
        };
        NifiTransferManagerConfiguration config = NifiTransferManagerConfiguration.Builder.newInstance().url(nifiContentlistenerHost)
                .build();
        typeManager.registerTypes(DataRequest.class);

        vault = mock(MockType.NICE, Vault.class);
        var nifiAuth = propOrEnv("NIFI_API_AUTH", null);
        if (nifiAuth == null) {
            throw new RuntimeException("No environment variable found NIFI_API_AUTH!");
        }
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(nifiAuth);

        var tokenJson = typeManager.writeValueAsString(new AzureSasToken(sharedAccessSignature, 0));
        expect(vault.resolveSecret(storageAccount + "-key1")).andReturn(tokenJson).anyTimes();

        var token = Map.of("accessKeyId", "AKIAY2XSTIMWG2HEKF77", "secretAccessKey", "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", "sessionToken", "");
        expect(vault.resolveSecret(s3BucketName)).andReturn(typeManager.writeValueAsString(token)).anyTimes();

        replay(vault);
        SchemaRegistry registry = new SchemaRegistryImpl();
        registry.register(new AzureBlobStoreSchema());
        registry.register(new S3BucketSchema());
        controller = new NifiDataFlowController(config, typeManager, monitor, vault, httpClient, new NifiTransferEndpointConverter(registry, vault, typeManager));
    }

    @Test
    @Timeout(value = 60)
    @DisplayName("transfer with Atlas catalog")
    void initiateFlow_withAtlasCatalog() throws InterruptedException {

        // create custom atlas type and an instance
        String id;
        var schema = new AzureBlobStoreSchema();
        AtlasApi atlasApi = new AtlasApiImpl(new AtlasClientV2(new String[]{atlasApiUrl}, new String[]{atlasUsername, atlasPassword}));
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
    @DisplayName("transfer with In-Mem catalog")
    void initiateFlow_withInMemCatalog() throws InterruptedException {

        String id = UUID.randomUUID().toString();
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().id(id).catalog(createAzureCatalogEntry()).build();

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
    @DisplayName("Don't transfer if source not found")
    void initiateFlow_sourceNotFound() {
        String id = UUID.randomUUID().toString();
        GenericDataCatalog lookup = createAzureCatalogEntry();
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
    @DisplayName("Don't transfer if no creds are found in vault")
    void initiateFlow_noCredsFoundInVault() {
        String id = UUID.randomUUID().toString();
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().catalog(createAzureCatalogEntry()).build();

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

    @Test
    @Timeout(10)
    @DisplayName("transfer from Azure Blob to S3")
    void transfer_fromAzureBlob_toS3() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().id(id).catalog(createAzureCatalogEntry()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AmazonS3")
                        .property("region", "us-east-1")
                        .property("bucketName", s3BucketName)
                        .property("objectName", "bike_very_new.jpg")
                        .property("keyName", s3BucketName)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listS3BucketContents().stream().noneMatch(blob -> blob.key().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listS3BucketContents().stream().anyMatch(bi -> bi.key().equals("bike_very_new.jpg"))).isTrue();
    }

    @Test
    @Timeout(10)
    @DisplayName("transfer from S3 to Azure Blob")
    void transfer_fromS3_toAzureBlob() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().id(id).catalog(createS3CatalogEntry()).build();

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
    @Timeout(10)
    @DisplayName("transfer from S3 to S3")
    void transfer_fromS3_toS3() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().id(id).catalog(createS3CatalogEntry()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AmazonS3")
                        .property("region", "us-east-1")
                        .property("bucketName", s3BucketName)
                        .property("objectName", "bike_very_new.jpg")
                        .property("keyName", s3BucketName)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listS3BucketContents().stream().noneMatch(blob -> blob.key().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listS3BucketContents().stream().anyMatch(bi -> bi.key().equals("bike_very_new.jpg"))).isTrue();
    }

    @Test
    @Timeout(10)
    @DisplayName("transfer from Azure Blob to Azure blob")
    void transfer_fromAzureBlob_toAzureBlob() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        DataEntry<DataCatalog> entry = DataEntry.Builder.newInstance().id(id).catalog(createAzureCatalogEntry()).build();

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

    private GenericDataCatalog createAzureCatalogEntry() {
        return GenericDataCatalog.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("account", storageAccount)
                .property("container", containerName)
                .property("blobname", blobName)
                .property("keyName", storageAccount + "-key1")
                .build();

    }

    private GenericDataCatalog createS3CatalogEntry() {
        return GenericDataCatalog.Builder.newInstance()
                .property("type", "AmazonS3")
                .property("region", "us-east-1")
                .property("bucketName", s3BucketName)
                .property("objectName", blobName)
                .property("keyName", s3BucketName)
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
