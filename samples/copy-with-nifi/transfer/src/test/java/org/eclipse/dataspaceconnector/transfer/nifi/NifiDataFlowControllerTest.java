/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.nifi;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.easymock.MockType;
import org.eclipse.dataspaceconnector.catalog.atlas.metadata.AtlasDataCatalogEntry;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.dataseed.nifi.api.NifiApiClient;
import org.eclipse.dataspaceconnector.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.monitor.MonitorProvider;
import org.eclipse.dataspaceconnector.provision.azure.AzureSasToken;
import org.eclipse.dataspaceconnector.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.schema.SchemaRegistryImpl;
import org.eclipse.dataspaceconnector.schema.azure.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.schema.s3.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.GenericDataCatalogEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.*;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.easymock.EasyMock.*;
import static org.eclipse.dataspaceconnector.common.ConfigurationFunctions.propOrEnv;
import static org.junit.jupiter.api.Assertions.assertEquals;

@IntegrationTest
public class NifiDataFlowControllerTest {

    private static final String DEFAULT_NIFI_HOST = "http://localhost";
    private final static String storageAccount = "storageitest";
    private final static String atlasUsername = "admin";
    private final static String atlasPassword = "admin";
    private static String nifiFlowHost;
    private static String s3BucketName = "dataspaceconnector-itest";
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

        var isCi = propOrEnv("RUN_INTEGRATION_TEST", "false");
        if (!Boolean.parseBoolean(isCi)) {
            return;
        }

        NifiDataFlowControllerTest.monitor = new ConsoleMonitor();
        MonitorProvider.setInstance(NifiDataFlowControllerTest.monitor);

        var host = propOrEnv("EDC_NIFI_URL", NifiDataFlowControllerTest.DEFAULT_NIFI_HOST);

        NifiDataFlowControllerTest.nifiApiHost = host + ":8080";
        NifiDataFlowControllerTest.nifiFlowHost = host + ":8888";

        NifiDataFlowControllerTest.s3AccessKeyId = propOrEnv("S3_ACCESS_KEY_ID", null);
        if (NifiDataFlowControllerTest.s3AccessKeyId == null) {
            throw new RuntimeException("S3_ACCESS_KEY_ID cannot be null!");
        }
        NifiDataFlowControllerTest.s3SecretAccessKey = propOrEnv("S3_SECRET_ACCESS_KEY", null);
        if (NifiDataFlowControllerTest.s3SecretAccessKey == null) {
            throw new RuntimeException("S3_SECRET_ACCESS_KEY cannot be null!");
        }

        NifiDataFlowControllerTest.typeManager = new TypeManager();
        NifiDataFlowControllerTest.typeManager.getMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        NifiDataFlowControllerTest.httpClient = new OkHttpClient.Builder().build();

        var f = Thread.currentThread().getContextClassLoader().getResource("ThreeClouds.xml");
        var file = new File(Objects.requireNonNull(f).toURI());
        NifiApiClient client = new NifiApiClient(NifiDataFlowControllerTest.nifiApiHost, NifiDataFlowControllerTest.typeManager, NifiDataFlowControllerTest.httpClient);
        String processGroup = "root";

        try {
            System.out.println("prepare - uploading template to nifi");
            var templateId = client.uploadTemplate(processGroup, file);
            System.out.println("prepare - instantiate template in nifi");
            client.instantiateTemplate(templateId);
            System.out.println("prepare - setup variable registry");
        } catch (EdcException ignored) {
        } finally {
            System.out.println("prepare - start controller service");
            var controllerService = client.getControllerServices(processGroup).get(0);
            var version = controllerService.revision.version;
            var controllerServiceId = controllerService.id;
            client.startControllerService(controllerServiceId, version);
            System.out.println("prepare - start process group");
            client.startProcessGroup(processGroup);

        }

        NifiDataFlowControllerTest.blobName = "testimage.jpg";
        NifiDataFlowControllerTest.secondBlobName = "secondFile.txt";

        //prepare bucket, i.e, upload test file to bucket
        System.out.println("prepare - create S3 bucket");

        UUID testId = UUID.randomUUID();
        NifiDataFlowControllerTest.s3BucketName += "-" + testId;
        NifiDataFlowControllerTest.s3client = S3Client.builder().region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return NifiDataFlowControllerTest.s3AccessKeyId;
                    }

                    @Override
                    public String secretAccessKey() {
                        return NifiDataFlowControllerTest.s3SecretAccessKey;
                    }
                })).build();
        NifiDataFlowControllerTest.createBucket(NifiDataFlowControllerTest.s3BucketName);

        System.out.println("prepare - upload image to S3 bucket");

        NifiDataFlowControllerTest.uploadFileToS3Bucket(NifiDataFlowControllerTest.s3BucketName, NifiDataFlowControllerTest.blobName);
        NifiDataFlowControllerTest.uploadFileToS3Bucket(NifiDataFlowControllerTest.s3BucketName, NifiDataFlowControllerTest.secondBlobName);

        // create azure storage container
        NifiDataFlowControllerTest.containerName = "dataspaceconnector-itest-" + testId;

        NifiDataFlowControllerTest.sharedAccessSignature = propOrEnv("AZ_STORAGE_SAS", null);
        if (NifiDataFlowControllerTest.sharedAccessSignature == null) {
            throw new RuntimeException("No environment variable found AZ_STORAGE_SAS!");
        }

        System.out.println("prepare - construct blobservice client");

        NifiDataFlowControllerTest.blobServiceClient = new BlobServiceClientBuilder().sasToken(NifiDataFlowControllerTest.sharedAccessSignature)
                .endpoint("https://" + NifiDataFlowControllerTest.storageAccount + ".blob.core.windows.net")
                .buildClient();

        // upload blob to storage
        System.out.println("prepare - upload test file to blob store");

        NifiDataFlowControllerTest.uploadFileToBlobContainer(NifiDataFlowControllerTest.containerName, NifiDataFlowControllerTest.blobName);
        NifiDataFlowControllerTest.uploadFileToBlobContainer(NifiDataFlowControllerTest.containerName, NifiDataFlowControllerTest.secondBlobName);

        System.out.println("prepare - done");
    }

    private static void createBucket(String bucketName) {
        NifiDataFlowControllerTest.s3client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    }

    @AfterAll
    public static void oneTimeTeardown() {
        System.out.println("oneTimeTeardown - clean blobs");

        NifiDataFlowControllerTest.deleteContainer(NifiDataFlowControllerTest.containerName);

        NifiDataFlowControllerTest.deleteBucket(NifiDataFlowControllerTest.s3BucketName);
    }

    private static void deleteContainer(String containerName) {
        BlobContainerClient cc = NifiDataFlowControllerTest.blobServiceClient.getBlobContainerClient(containerName);
        if (cc.exists()) {
            cc.delete();
        }
    }

    private static List<S3Object> listS3BucketContents(String bucketName) {
        return NifiDataFlowControllerTest.s3client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .build())
                .contents();
    }

    private static void uploadFileToS3Bucket(String bucketName, String filename) throws URISyntaxException {
        URL testImageStream = Thread.currentThread().getContextClassLoader().getResource(filename);
        RequestBody requestBody = RequestBody.fromFile(Path.of(testImageStream.toURI()));
        PutObjectResponse putObjectResponse = NifiDataFlowControllerTest.s3client.putObject(PutObjectRequest.builder().bucket(bucketName).key(filename).build(), requestBody);
        String s = putObjectResponse.eTag();
    }

    private static void uploadFileToBlobContainer(String containerName, String blobName) throws URISyntaxException {
        BlobContainerClient cc = NifiDataFlowControllerTest.blobServiceClient.getBlobContainerClient(containerName);

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
                    var objects = NifiDataFlowControllerTest.listS3BucketContents(destBucketName);
                    for (var obj : objects) {
                        System.out.println("  Delete Bucket object " + obj.key());
                        NifiDataFlowControllerTest.s3client.deleteObject(DeleteObjectRequest.builder().bucket(destBucketName).key(obj.key()).build());
                    }
                    System.out.println("oneTimeTeardown - delete bucket");
                    NifiDataFlowControllerTest.s3client.deleteBucket(rq);
                });
    }

    @BeforeEach
    void setUp() {

        System.out.println("");


        NifiTransferManagerConfiguration config = NifiTransferManagerConfiguration.Builder.newInstance()
                .url(NifiDataFlowControllerTest.nifiApiHost)
                .flowUrl(NifiDataFlowControllerTest.nifiFlowHost)
                .build();
        typeManager.registerTypes(DataRequest.class);

        vault = mock(MockType.NICE, Vault.class);
        var nifiAuth = propOrEnv("NIFI_API_AUTH", null);
        if (nifiAuth == null) {
            throw new RuntimeException("No environment variable found NIFI_API_AUTH!");
        }
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(nifiAuth);

        var tokenJson = NifiDataFlowControllerTest.typeManager.writeValueAsString(new AzureSasToken(NifiDataFlowControllerTest.sharedAccessSignature, 0));
        expect(vault.resolveSecret(NifiDataFlowControllerTest.storageAccount + "-key1")).andReturn(tokenJson).anyTimes();

        var token = Map.of("accessKeyId", NifiDataFlowControllerTest.s3AccessKeyId, "secretAccessKey", NifiDataFlowControllerTest.s3SecretAccessKey, "sessionToken", "");
        expect(vault.resolveSecret(NifiDataFlowControllerTest.s3BucketName)).andReturn(NifiDataFlowControllerTest.typeManager.writeValueAsString(token)).anyTimes();

        replay(vault);
        SchemaRegistry registry = new SchemaRegistryImpl();
        registry.register(new AzureBlobStoreSchema());
        registry.register(new S3BucketSchema());
        controller = new NifiDataFlowController(config, NifiDataFlowControllerTest.typeManager, NifiDataFlowControllerTest.monitor, vault, NifiDataFlowControllerTest.httpClient, new NifiTransferEndpointConverter(registry, vault, NifiDataFlowControllerTest.typeManager));
        NifiDataFlowControllerTest.blobServiceClient = new BlobServiceClientBuilder().sasToken(NifiDataFlowControllerTest.sharedAccessSignature)
                .endpoint("https://" + NifiDataFlowControllerTest.storageAccount + ".blob.core.windows.net")
                .buildClient();
        NifiDataFlowControllerTest.secondBlobName = "secondFile.txt";
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
                .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
                .property("blobname", NifiDataFlowControllerTest.blobName)
                .property("container", NifiDataFlowControllerTest.containerName)
                .property("account", NifiDataFlowControllerTest.storageAccount)
                .build());
        DataEntry entry = DataEntry.Builder.newInstance().id(id).catalogEntry(lookup).build();

        // connect the "source" (i.e. the lookup) and the "destination"
        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("account", NifiDataFlowControllerTest.storageAccount)
                        .property("container", NifiDataFlowControllerTest.containerName)
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());

        // will fail if new blob is not there after 60 seconds
        while (listBlobs(NifiDataFlowControllerTest.containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();

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
                        .property("account", NifiDataFlowControllerTest.storageAccount)
                        .property("container", NifiDataFlowControllerTest.containerName)
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs(NifiDataFlowControllerTest.containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();

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
                        .property("account", NifiDataFlowControllerTest.storageAccount)
                        .property("container", NifiDataFlowControllerTest.containerName)
                        .property("blobname", "will_not_succeed.jpg")
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
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
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1").build())
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
                        .property("bucketName", NifiDataFlowControllerTest.s3BucketName)
                        .property("keyName", NifiDataFlowControllerTest.s3BucketName)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (NifiDataFlowControllerTest.listS3BucketContents(NifiDataFlowControllerTest.s3BucketName).stream().noneMatch(blob -> blob.key().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        Thread.sleep(1000);
        assertThat(NifiDataFlowControllerTest.listS3BucketContents(NifiDataFlowControllerTest.s3BucketName).stream().anyMatch(bi -> bi.key().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        assertThat(NifiDataFlowControllerTest.listS3BucketContents(NifiDataFlowControllerTest.s3BucketName).stream().anyMatch(bi -> bi.key().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();
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
                        .property("account", NifiDataFlowControllerTest.storageAccount)
                        .property("container", NifiDataFlowControllerTest.containerName)
                        .property("blobname", "bike_very_new.jpg")
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs(NifiDataFlowControllerTest.containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();
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
                        .property("bucketName", NifiDataFlowControllerTest.s3BucketName)
                        .property("keyName", NifiDataFlowControllerTest.s3BucketName)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (NifiDataFlowControllerTest.listS3BucketContents(NifiDataFlowControllerTest.s3BucketName).stream().noneMatch(blob -> blob.key().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(NifiDataFlowControllerTest.listS3BucketContents(NifiDataFlowControllerTest.s3BucketName).stream().anyMatch(bi -> bi.key().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        assertThat(NifiDataFlowControllerTest.listS3BucketContents(NifiDataFlowControllerTest.s3BucketName).stream().anyMatch(bi -> bi.key().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();
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
                        .property("account", NifiDataFlowControllerTest.storageAccount)
                        .property("container", NifiDataFlowControllerTest.containerName)
                        .property("blobname", "bike_very_new.jpg")
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs(NifiDataFlowControllerTest.containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();
    }

    @Test
    @Timeout(20)
    @DisplayName("Transfer several files from Azure Blob to Azure Blob")
    void transfer_multiple_fromAzureToAzure() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry dataEntry = createAzureCatalogEntry();


        var destContainerName = "dataspaceconnector-nifi-dest-" + System.currentTimeMillis();
        BlobContainerClient destinationContainer = NifiDataFlowControllerTest.blobServiceClient.createBlobContainer(destContainerName);


        dataEntry.getProperties().replace("blobname", "[\"" + NifiDataFlowControllerTest.blobName + "\", \"" + NifiDataFlowControllerTest.secondBlobName + "\"]");
        DataEntry entry = DataEntry.Builder.newInstance()
                .id(id)
                .catalogEntry(dataEntry)
                .build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type(AzureBlobStoreSchema.TYPE)
                        .property("account", NifiDataFlowControllerTest.storageAccount)
                        .property("container", destContainerName)
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
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
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();
        destinationContainer.delete();
    }

    @Test
    @Timeout(20)
    @DisplayName("Transfer multiple files from S3 to Azure Blob")
    void transfer_multiple_fromS3_toAzure() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry dataEntry = createS3CatalogEntry();

        dataEntry.getProperties().replace("blobname", "[\"" + NifiDataFlowControllerTest.blobName + "\", \"" + NifiDataFlowControllerTest.secondBlobName + "\"]");

        DataEntry entry = DataEntry.Builder.newInstance()
                .id(id)
                .catalogEntry(dataEntry)
                .build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("account", NifiDataFlowControllerTest.storageAccount)
                        .property("container", NifiDataFlowControllerTest.containerName)
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        while (listBlobs(NifiDataFlowControllerTest.containerName).stream().noneMatch(blob -> blob.getName().equals(id + ".complete"))) {
            Thread.sleep(500);
        }
        assertThat(listBlobs(NifiDataFlowControllerTest.containerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.blobName))).isTrue();
//        assertThat(listBlobs(containerName).stream().anyMatch(bi -> bi.getName().equals(secondBlobName))).isTrue();
    }

    @Test
    @Timeout(20)
    @DisplayName("Transfer all files from Azure Blob to Azure Blob")
    void transfer_all_fromAzureToAzure() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry dataEntry = createAzureCatalogEntry();

        var destContainerName = "dataspaceconnector-nifi-dest-" + System.currentTimeMillis();
        BlobContainerClient container = NifiDataFlowControllerTest.blobServiceClient.createBlobContainer(destContainerName);


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
                        .property("account", NifiDataFlowControllerTest.storageAccount)
                        .property("container", destContainerName)
                        .keyName(NifiDataFlowControllerTest.storageAccount + "-key1")
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
        assertThat(listBlobs(destContainerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();
        assertThat(listBlobs(destContainerName).stream().anyMatch(bi -> bi.getName().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        container.delete();
    }

    @Test
    @Timeout(20)
    @DisplayName("Transfer all files from S3 to S3")
    void transfer_all_fromS3_ToS3() throws InterruptedException {
        String id = UUID.randomUUID().toString();
        GenericDataCatalogEntry dataEntry = createS3CatalogEntry();

        var destBucketName = "dataspaceconnector-nifi-dest-" + System.currentTimeMillis();
        NifiDataFlowControllerTest.createBucket(destBucketName);


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
                        .property("keyName", NifiDataFlowControllerTest.s3BucketName)
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());


        // will fail if new blob is not there after 10 seconds
        var contents = NifiDataFlowControllerTest.listS3BucketContents(destBucketName);
        while (contents.stream().noneMatch(blob -> blob.key().equals(NifiDataFlowControllerTest.secondBlobName)) || contents.stream().noneMatch(blob -> blob.key().equals(NifiDataFlowControllerTest.blobName))) {
            Thread.sleep(500);
            contents = NifiDataFlowControllerTest.listS3BucketContents(destBucketName);
        }

        contents = NifiDataFlowControllerTest.listS3BucketContents(destBucketName);

        assertThat(contents.stream().anyMatch(bi -> bi.key().equals(NifiDataFlowControllerTest.secondBlobName))).isTrue();
        assertThat(contents.stream().anyMatch(bi -> bi.key().equals(NifiDataFlowControllerTest.blobName))).isTrue();
        NifiDataFlowControllerTest.deleteBucket(destBucketName);
    }

    /// HELPERS ///
    private GenericDataCatalogEntry createAzureCatalogEntry() {
        return GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "AzureStorage")
                .property("account", NifiDataFlowControllerTest.storageAccount)
                .property("container", NifiDataFlowControllerTest.containerName)
                .property("keyName", NifiDataFlowControllerTest.storageAccount + "-key1")
                .build();

    }

    private GenericDataCatalogEntry createS3CatalogEntry() {
        return GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "AmazonS3")
                .property("region", "us-east-1")
                .property("bucketName", NifiDataFlowControllerTest.s3BucketName)
                .property("keyName", NifiDataFlowControllerTest.s3BucketName)
                .build();
    }

    private PagedIterable<BlobItem> listBlobs(String containerName) {
        var connectionString = "BlobEndpoint=https://" + NifiDataFlowControllerTest.storageAccount + ".blob.core.windows.net/;SharedAccessSignature=" + NifiDataFlowControllerTest.sharedAccessSignature;
        var bsc = new BlobServiceClientBuilder().connectionString(connectionString)
                .buildClient();
        var bcc = bsc.getBlobContainerClient(containerName);
        return bcc.listBlobs();
    }
}
