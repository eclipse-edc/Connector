package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.transform;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TransferRequestDtoToDataRequestTransformerTest {

    private static Faker faker = new Faker();
    private DtoTransformer<TransferRequestDto, DataRequest> transformer = new TransferRequestDtoToDataRequestTransformer();

    @Test
    void getInputType() {
        assertThat(transformer.getInputType()).isEqualTo(TransferRequestDto.class);
    }

    @Test
    void getOutputType() {
        assertThat(transformer.getOutputType()).isEqualTo(DataRequest.class);
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var transferReq = transferRequestDto();
        var dataRequest = transformer.transform(transferReq, context);
        assertThat(dataRequest.getAssetId()).isEqualTo(transferReq.getAssetId());
        assertThat(dataRequest.getConnectorAddress()).isEqualTo(transferReq.getConnectorAddress());
        assertThat(dataRequest.getConnectorId()).isEqualTo(transferReq.getConnectorId());
        assertThat(dataRequest.getDataDestination()).isEqualTo(transferReq.getDataDestination());
        assertThat(dataRequest.getDestinationType()).isEqualTo(transferReq.getDataDestination().getType());
        assertThat(dataRequest.getContractId()).isEqualTo(transferReq.getContractId());
        assertThat(dataRequest.getProtocol()).isEqualTo(transferReq.getProtocol());
        assertThat(dataRequest.getProperties()).isEqualTo(transferReq.getProperties());
        assertThat(dataRequest.getTransferType()).isEqualTo(transferReq.getTransferType());
        assertThat(dataRequest.isManagedResources()).isEqualTo(transferReq.isManagedResources());
    }

    private TransferRequestDto transferRequestDto() {
        return TransferRequestDto.Builder.newInstance()
                .connectorAddress(faker.internet().url())
                .assetId(faker.lorem().word())
                .contractId(faker.lorem().word())
                .protocol(faker.lorem().word())
                .dataDestination(DataAddress.Builder.newInstance().type(faker.lorem().word()).build())
                .connectorId(faker.lorem().word())
                .properties(Map.of(faker.lorem().word(), faker.lorem().word()))
                .build();
    }

}