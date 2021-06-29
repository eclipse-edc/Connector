/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.microsoft.dagx.catalog.atlas.metadata.AtlasDataCatalogEntry;
import com.microsoft.dagx.dataseed.nifi.api.NifiApiClient;
import com.microsoft.dagx.monitor.ConsoleMonitor;
import com.microsoft.dagx.monitor.MonitorProvider;
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
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataCatalogEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.transfer.provision.azure.AzureSasToken;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.microsoft.dagx.common.ConfigurationFunctions.propOrEnv;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
public class NifiDataFlowControllerTest {

    private static final String DEFAULT_NIFI_HOST = "http://localhost";
    private final static String storageAccount = "dagxblobstoreitest";
    private final static String atlasUsername = "admin";
    private final static String atlasPassword = "admin";
    private static String nifiFlowHost;
    private static String s3BucketName = "dagx-itest";
    private static String s3AccessKeyId;
    private static String s3SecretAccessKey;
    private static String sharedAccessSignature = null;
    private static String blobName;
    private static OkHttpClient httpClient;
    private static TypeManager typeManager;
    private static String containerName;
    private static S3Client s3client;
    private static BlobServiceClient blobServiceClient;
    private static String secondBlobName;
    private static Monitor monitor;
    private static String nifiApiHost;
    private NifiDataFlowController controller;
    private Vault vault;

    @BeforeAll
    public static void oneTimeSetup() throws Exception {

        System.out.println("prepare");

//         this is necessary because the @EnabledIf... annotation does not prevent @BeforeAll to be called
        var isCi = propOrEnv("CI", "false");
        if (!Boolean.parseBoolean(isCi)) {
            return;
        }

        monitor = new ConsoleMonitor();
        MonitorProvider.setInstance(monitor);

        var host = propOrEnv("NIFI_URL", DEFAULT_NIFI_HOST);

        nifiApiHost = host + ":8080";
        nifiFlowHost = host + ":8888";

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

        var f = Thread.currentThread().getContextClassLoader().getResource("ThreeClouds.xml");
        var file = new File(Objects.requireNonNull(f).toURI());
        NifiApiClient client = new NifiApiClient(nifiApiHost, typeManager, httpClient);
        String processGroup = "root";

        try {
            System.out.println("prepare - uploading template to nifi");
            var templateId = client.uploadTemplate(processGroup, file);
            System.out.println("prepare - instantiate template in nifi");
            client.instantiateTemplate(templateId);
            System.out.println("prepare - setup variable registry");
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
        secondBlobName = "secondFile.txt";

        //prepare bucket, i.e, upload test file to bucket
        System.out.println("prepare - create S3 bucket");

        UUID testId = UUID.randomUUID();
        s3BucketName += "-" + testId;
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
        createBucket(s3BucketName);

        System.out.println("prepare - upload image to S3 bucket");

        uploadFileToS3Bucket(s3BucketName, blobName);
        uploadFileToS3Bucket(s3BucketName, secondBlobName);

        // create azure storage container
        containerName = "dagx-itest-" + testId;

        sharedAccessSignature = propOrEnv("AZ_STORAGE_SAS", null);
        if (sharedAccessSignature == null) {
            throw new RuntimeException("No environment variable found AZ_STORAGE_SAS!");
        }

        System.out.println("prepare - construct blobservice client");

        blobServiceClient = new BlobServiceClientBuilder().sasToken(sharedAccessSignature)
                .endpoint("https://" + storageAccount + ".blob.core.windows.net")
                .buildClient();

        // upload blob to storage
        System.out.println("prepare - upload test file to blob store");

        uploadFileToBlobContainer(containerName, blobName);
        uploadFileToBlobContainer(containerName, secondBlobName);

        System.out.println("prepare - done");
    }

    private static void createBucket(String bucketName) {
        s3client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    }

    @AfterAll
    public static void oneTimeTeardown() {
        System.out.println("oneTimeTeardown - clean blobs");

        deleteContainer(containerName);

        deleteBucket(s3BucketName);
    }

    private static void deleteContainer(String containerName) {
        BlobContainerClient cc = blobServiceClient.getBlobContainerClient(containerName);
        if (cc.exists()) {
            cc.delete();
        }
    }

    private static List<S3Object> listS3BucketContents(String bucketName) {
        return s3client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build())
                .contents();
    }

    private static void uploadFileToS3Bucket(String bucketName, String filename) throws URISyntaxException {
        URL testImageStream = Thread.currentThread().getContextClassLoader().getResource(filename);
        RequestBody requestBody = RequestBody.fromFile(Path.of(testImageStream.toURI()));
        PutObjectResponse putObjectResponse = s3client.putObject(PutObjectRequest.builder().bucket(bucketName).key(filename).build(), requestBody);
        String s = putObjectResponse.eTag();
    }

    private static void uploadFileToBlobContainer(String containerName, String blobName) throws URISyntaxException {
        BlobContainerClient cc = blobServiceClient.getBlobContainerClient(containerName);

        if (!cc.exists()) {
            cc.create();
        }
        var blobClient = cc.getBlobClient(blobName);
        URL testImageStream = Thread.currentThread().getContextClassLoader().getResource(NifiDataFlowControllerTest.blobName);
        String absolutePath = Objects.requireNonNull(Paths.get(testImageStream.toURI())).toString();
        blobClient.uploadFromFile(absolutePath, true);

    }

    private static void deleteBucket(String destBucketName) {
        DeleteBucketRequest rq = DeleteBucketRequest.builder().bucket(destBucketName).build();
        Failsafe.with(new RetryPolicy<>().withMaxRetries(5)
                .handle(S3Exception.class)
                .withDelay(Duration.ofMillis(1000)))
                .run(() -> {
                    System.out.println("oneTimeTeardown - clean bucket");
                    var objects = listS3BucketContents(destBucketName);
                    for (var obj : objects) {
                        System.out.println("  Delete Bucket object " + obj.key());
                        s3client.deleteObject(DeleteObjectRequest.builder().bucket(destBucketName).key(obj.key()).build());
                    }
                    System.out.println("oneTimeTeardown - delete bucket");
                    s3client.deleteBucket(rq);
                });
    }

    @BeforeEach
    void setUp() {

        System.out.println("");


        NifiTransferManagerConfiguration config = NifiTransferManagerConfiguration.Builder.newInstance()
                .url(nifiApiHost)
                .flowUrl(nifiFlowHost)
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

        var token = Map.of("accessKeyId", s3AccessKeyId, "secretAccessKey", s3SecretAccessKey, "sessionToken", "");
        expect(vault.resolveSecret(s3BucketName)).andReturn(typeManager.writeValueAsString(token)).anyTimes();

        replay(vault);
        SchemaRegistry registry = new SchemaRegistryImpl();
        registry.register(new AzureBlobStoreSchema());
        registry.register(new S3BucketSchema());
        controller = new NifiDataFlowController(config, typeManager, monitor, vault, httpClient, new NifiTransferEndpointConverter(registry, vault, typeManager));
        blobServiceClient = new BlobServiceClientBuilder().sasToken(sharedAccessSignature)
                .endpoint("https://" + storageAccount + ".blob.core.windows.net")
                .buildClient();
        secondBlobName = "secondFile.txt";
    }

    @Test
    @Timeout(value = 60)
    @DisplayName("transfer with Atlas catalog")
    void initiateFlow_withAtlasCatalog() throws InterruptedException {

        // create custom atlas type and an instance
        String id = UUID.randomUUID().toString();

        // perform the actual source file properties in Apache Atlas
        var lookup = new AtlasDataCatalogEntry(DataAddress.Builder.newInstance()
                .type(AzureBlobStoreSchema.TYPE)
                .keyName(storageAccount + "-key1")
                .property("blobname", blobName)
                .property("container", containerName)
                .property("account", storageAccount)
                .build());
        DataEntry entry = DataEntry.Builder.newInstance().id(id).catalogEntry(lookup).build();

        // connect the "source" (i.e. the lookup) and the "destination"
        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("account", storageAccount)
                        .property("container", containerName)
                        .keyName(storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());

        // will fail if new blob is not there after 60 seconds
        while (listBlobs(containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(blobName))).isTrue();
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(secondBlobName))).isTrue();

    }

    @Test
    @Timeout(value = 10)
    @DisplayName("transfer with In-Mem catalog")
    void initiateFlow_withInMemCatalog() throws InterruptedException {

        String id = UUID.randomUUID().toString();
        DataEntry entry = DataEntry.Builder.newInstance().id(id).catalogEntry(createAzureCatalogEntry()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("account", storageAccount)
                        .property("container", containerName)
                        .keyName(storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs(containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(blobName))).isTrue();
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(secondBlobName))).isTrue();

    }

    @Test
    @DisplayName("Don't transfer if source not found")
    void initiateFlow_sourceNotFound() {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry lookup = createAzureCatalogEntry();
        lookup.getProperties().replace("blobname", "notexist.png");
        DataEntry entry = DataEntry.Builder.newInstance()
                .id(id)
                .catalogEntry(lookup)
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
        DataEntry entry = DataEntry.Builder.newInstance().catalogEntry(createAzureCatalogEntry()).build();

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
        DataEntry entry = DataEntry.Builder.newInstance().id(id).catalogEntry(createAzureCatalogEntry()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AmazonS3")
                        .property("region", "us-east-1")
                        .property("bucketName", s3BucketName)
                        .property("keyName", s3BucketName)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listS3BucketContents(s3BucketName).stream().noneMatch(blob -> blob.key().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        Thread.sleep(1000);
        assertThat(listS3BucketContents(s3BucketName).stream().anyMatch(bi -> bi.key().equals(blobName))).isTrue();
        assertThat(listS3BucketContents(s3BucketName).stream().anyMatch(bi -> bi.key().equals(secondBlobName))).isTrue();
    }

    @Test
    @Timeout(10)
    @DisplayName("transfer from S3 to Azure Blob")
    void transfer_fromS3_toAzureBlob() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        DataEntry entry = DataEntry.Builder.newInstance().id(id).catalogEntry(createS3CatalogEntry()).build();

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
        while (listBlobs(containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(blobName))).isTrue();
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(secondBlobName))).isTrue();
    }

    @Test
    @Timeout(10)
    @DisplayName("transfer from S3 to S3")
    void transfer_fromS3_toS3() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        DataEntry entry = DataEntry.Builder.newInstance().id(id).catalogEntry(createS3CatalogEntry()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AmazonS3")
                        .property("region", "us-east-1")
                        .property("bucketName", s3BucketName)
                        .property("keyName", s3BucketName)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listS3BucketContents(s3BucketName).stream().noneMatch(blob -> blob.key().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listS3BucketContents(s3BucketName).stream().anyMatch(bi -> bi.key().equals(blobName))).isTrue();
        assertThat(listS3BucketContents(s3BucketName).stream().anyMatch(bi -> bi.key().equals(secondBlobName))).isTrue();
    }

    @Test
    @Timeout(10)
    @DisplayName("transfer from Azure Blob to Azure blob")
    void transfer_fromAzureBlob_toAzureBlob() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        DataEntry entry = DataEntry.Builder.newInstance().id(id).catalogEntry(createAzureCatalogEntry()).build();

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
        while (listBlobs(containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(blobName))).isTrue();
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(secondBlobName))).isTrue();
    }

    @Test
    @Timeout(20)
    @DisplayName("Transfer several files from Azure Blob to Azure Blob")
    void transfer_multiple_fromAzureToAzure() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry dataEntry = createAzureCatalogEntry();


        final var destContainerName = "dagx-nifi-dest-" + System.currentTimeMillis();
        BlobContainerClient destinationContainer = blobServiceClient.createBlobContainer(destContainerName);


        dataEntry.getProperties().replace("blobname", "[\"" + blobName + "\", \"" + secondBlobName + "\"]");
        DataEntry entry = DataEntry.Builder.newInstance()
                .id(id)
                .catalogEntry(dataEntry)
                .build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type(AzureBlobStoreSchema.TYPE)
                        .property("account", storageAccount)
                        .property("container", destContainerName)
                        .keyName(storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs(destContainerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(blobName))).isTrue();
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(secondBlobName))).isTrue();
        destinationContainer.delete();
    }

    @Test
    @Timeout(20)
    @DisplayName("Transfer multiple files from S3 to Azure Blob")
    void transfer_multiple_fromS3_toAzure() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry dataEntry = createS3CatalogEntry();

        dataEntry.getProperties().replace("blobname", "[\"" + blobName + "\", \"" + secondBlobName + "\"]");

        DataEntry entry = DataEntry.Builder.newInstance()
                .id(id)
                .catalogEntry(dataEntry)
                .build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("account", storageAccount)
                        .property("container", containerName)
                        .keyName(storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs(containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(blobName))).isTrue();
//        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(secondBlobName))).isTrue();
    }

    @Test
    @Timeout(20)
    @DisplayName("Transfer all files from Azure Blob to Azure Blob")
    void transfer_all_fromAzureToAzure() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry dataEntry = createAzureCatalogEntry();

        final var destContainerName = "dagx-nifi-dest-" + System.currentTimeMillis();
        BlobContainerClient container = blobServiceClient.createBlobContainer(destContainerName);


        dataEntry.getProperties().remove("blobname");
        DataEntry entry = DataEntry.Builder.newInstance()
                .id(id)
                .catalogEntry(dataEntry)
                .build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type(AzureBlobStoreSchema.TYPE)
                        .property("account", storageAccount)
                        .property("container", destContainerName)
                        .keyName(storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs(destContainerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(destContainerName).stream().anyMatch(bi -> bi.getName().equals(secondBlobName))).isTrue();
        assertThat(listBlobs(destContainerName).stream().anyMatch(bi -> bi.getName().equals(blobName))).isTrue();
        container.delete();
    }

    @Test
    @Timeout(20)
    @DisplayName("Transfer all files from S3 to S3")
    void transfer_all_fromS3_ToS3() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry dataEntry = createS3CatalogEntry();

        final var destBucketName = "dagx-nifi-dest-" + System.currentTimeMillis();
        createBucket(destBucketName);


        dataEntry.getProperties().remove("blobname");
        DataEntry entry = DataEntry.Builder.newInstance()
                .id(id)
                .catalogEntry(dataEntry)
                .build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type(S3BucketSchema.TYPE)
                        .property("region", "us-east-1")
                        .property("bucketName", destBucketName)
                        .property("keyName", s3BucketName)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        var contents = listS3BucketContents(destBucketName);
        while (contents.stream().noneMatch(blob -> blob.key().equals(secondBlobName)) || contents.stream().noneMatch(blob -> blob.key().equals(blobName))) {
            Thread.sleep(500);
            contents = listS3BucketContents(destBucketName);
        }

        contents = listS3BucketContents(destBucketName);

        assertThat(contents.stream().anyMatch(bi -> bi.key().equals(secondBlobName))).isTrue();
        assertThat(contents.stream().anyMatch(bi -> bi.key().equals(blobName))).isTrue();
        deleteBucket(destBucketName);
    }

    /// HELPERS ///
    private GenericDataCatalogEntry createAzureCatalogEntry() {
        return GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("account", storageAccount)
                .property("container", containerName)
                .property("keyName", storageAccount + "-key1")
                .build();

    }

    private GenericDataCatalogEntry createS3CatalogEntry() {
        return GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "AmazonS3")
                .property("region", "us-east-1")
                .property("bucketName", s3BucketName)
                .property("keyName", s3BucketName)
                .build();
    }

    private PagedIterable<BlobItem> listBlobs(String containerName) {
        var connectionString = "BlobEndpoint=https://" + storageAccount + ".blob.core.windows.net/;SharedAccessSignature=" + sharedAccessSignature;
        var bsc = new BlobServiceClientBuilder().connectionString(connectionString)
                .buildClient();
        var bcc = bsc.getBlobContainerClient(containerName);
        return bcc.listBlobs();
    }
}
