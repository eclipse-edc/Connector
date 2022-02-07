package org.eclipse.dataspaceconnector.dataplane.framework.pipeline;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineServiceImplTest {
    private PipelineService service;

    @Test
    void verifyTransfer() {
        var sourceFactory = mock(DataSourceFactory.class);
        var sinkFactory = mock(DataSinkFactory.class);

        var source = mock(DataSource.class);
        var sink = mock(DataSink.class);

        when(sourceFactory.canHandle(isA(DataFlowRequest.class))).thenReturn(true);
        when(sourceFactory.createSource(isA(DataFlowRequest.class))).thenReturn(source);
        when(sinkFactory.canHandle(isA(DataFlowRequest.class))).thenReturn(true);
        when(sinkFactory.createSink(isA(DataFlowRequest.class))).thenReturn(sink);
        when(sink.transfer(eq(source))).thenReturn(completedFuture(TransferResult.success()));

        var request = DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();

        service.registerFactory(sourceFactory);
        service.registerFactory(sinkFactory);

        service.transfer(request);

        verify(sink).transfer(eq(source));
    }

    @BeforeEach
    void setUp() {
        service = new PipelineServiceImpl();
    }
}
