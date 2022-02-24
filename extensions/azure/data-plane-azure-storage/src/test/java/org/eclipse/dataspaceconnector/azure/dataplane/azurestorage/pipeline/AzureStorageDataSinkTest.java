package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapter;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource.Part;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.TestCustomException;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createRequest;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createSharedKey;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureStorageDataSinkTest {

    static Faker faker = new Faker();
    Monitor monitor = mock(Monitor.class);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    BlobAdapterFactory blobAdapterFactory = mock(BlobAdapterFactory.class);
    DataFlowRequest.Builder request = createRequest(AzureBlobStoreSchema.TYPE);

    String accountName = createAccountName();
    String containerName = createContainerName();
    String sharedKey = createSharedKey();
    String blobName = createBlobName();
    String content = faker.lorem().sentence();

    Exception exception = new TestCustomException(faker.lorem().sentence());

    AzureStorageDataSink dataSink = AzureStorageDataSink.Builder.newInstance()
            .accountName(accountName)
            .containerName(containerName)
            .sharedKey(sharedKey)
            .requestId(request.build().getId())
            .blobAdapterFactory(blobAdapterFactory)
            .executorService(executor)
            .monitor(monitor)
            .build();
    BlobAdapter destination = mock(BlobAdapter.class);
    InputStreamDataSource part = new InputStreamDataSource(blobName, new ByteArrayInputStream(content.getBytes(UTF_8)));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        when(destination.getOutputStream()).thenReturn(output);
        when(blobAdapterFactory.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenReturn(destination);
    }

    @Test
    void transferParts_succeeds() {
        var result = dataSink.transferParts(List.of(part));
        assertThat(result.succeeded()).isTrue();
        assertThat(output.toString(UTF_8)).isEqualTo(content);
    }

    @Test
    void transferParts_whenBlobClientCreationFails_fails() {
        when(blobAdapterFactory.getBlobAdapter(
                accountName,
                containerName,
                blobName,
                sharedKey))
                .thenThrow(exception);
        assertThatTransferPartsFails(part, "Error creating blob for %s on account %s", blobName, accountName);
    }

    @Test
    void transferParts_whenWriteFails_fails() {
        when(destination.getOutputStream()).thenThrow(exception);
    }


    @Test
    void transferParts_whenReadFails_fails() {
        when(destination.getOutputStream()).thenThrow(exception);
        Part part = mock(Part.class);
        when(part.openStream()).thenThrow(exception);
        when(part.name()).thenReturn(blobName);
        assertThatTransferPartsFails(part, "Error reading blob %s", blobName);
    }

    @Test
    void transferParts_whenTransferFails_fails() throws Exception {
        InputStream input = mock(InputStream.class);
        when(input.transferTo(output)).thenThrow(exception);
        Part part = mock(Part.class);
        when(part.openStream()).thenReturn(input);
        when(part.name()).thenReturn(blobName);
        assertThatTransferPartsFails(part, "Error transferring blob for %s on account %s", blobName, accountName);
    }

    private void assertThatTransferPartsFails(Part part, String logMessage, Object... args) {
        String message = format(logMessage, args);
        var result = dataSink.transferParts(List.of(part));
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly(message);
        verify(monitor).severe(message, exception);
    }
}