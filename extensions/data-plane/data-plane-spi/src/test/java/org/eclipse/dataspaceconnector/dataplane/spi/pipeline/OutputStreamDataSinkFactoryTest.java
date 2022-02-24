package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OutputStreamDataSinkFactoryTest {

    private OutputStreamDataSinkFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OutputStreamDataSinkFactory();
    }

    @Test
    void verifyCanHandle() {
        assertThat(factory.canHandle(createDataFlowRequest(OutputStreamDataSinkFactory.TYPE)))
                .isTrue();

        assertThat(factory.canHandle(createDataFlowRequest("dummy")))
                .isFalse();
    }

    @Test
    void validate() {
        assertThat(factory.validate(createDataFlowRequest(OutputStreamDataSinkFactory.TYPE)))
                .satisfies(result -> assertThat(result.succeeded()).isTrue());

        assertThat(factory.validate(createDataFlowRequest("dummy")))
                .satisfies(result -> {
                    assertThat(result.failed()).isTrue();
                    assertThat(result.getFailureMessages())
                            .containsExactly("OutputStreamDataSinkFactory: Cannot handle destination data address with type: dummy");
                });
    }

    @Test
    void verifyCreateSinkReturnCompletedFuture() {
        var sink = factory.createSink(null);
        assertThat(sink.transfer(null)).succeedsWithin(500L, TimeUnit.MILLISECONDS);
    }

    private static DataFlowRequest createDataFlowRequest(String destAddressType) {
        return DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type("")
                        .build())
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(destAddressType)
                        .build())
                .build();
    }
}