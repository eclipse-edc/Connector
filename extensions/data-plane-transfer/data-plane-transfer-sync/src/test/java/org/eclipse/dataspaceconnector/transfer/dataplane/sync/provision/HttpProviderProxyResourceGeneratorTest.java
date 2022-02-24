package org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HttpProviderProxyResourceGeneratorTest {

    private HttpProviderProxyResourceGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new HttpProviderProxyResourceGenerator();
    }

    @Test
    void verifyGenerate() {
        var tp = createTransferProcess();

        var rd = generator.generate(tp);

        assertThat(rd).isInstanceOf(HttpProviderProxyResourceDefinition.class);
        assertThat((HttpProviderProxyResourceDefinition) rd).satisfies(definition -> {
            assertThat(definition.getTransferProcessId()).isEqualTo(tp.getId());
            assertThat(definition.getId()).isNotNull();
            assertThat(definition.getAssetId()).isEqualTo(tp.getDataRequest().getAssetId());
            assertThat(definition.getContractId()).isEqualTo(tp.getDataRequest().getContractId());
            assertThat(definition.getType()).isEqualTo(tp.getDataRequest().getDestinationType());
        });
    }

    private static TransferProcess createTransferProcess() {
        return TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataRequest(DataRequest.Builder.newInstance()
                        .dataDestination(DataAddress.Builder.newInstance().type("test").build())
                        .assetId("asset-test")
                        .contractId("contract-test")
                        .destinationType("type-test")
                        .build())
                .build();
    }
}