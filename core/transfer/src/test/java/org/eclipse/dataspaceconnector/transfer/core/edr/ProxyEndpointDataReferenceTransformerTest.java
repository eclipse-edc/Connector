package org.eclipse.dataspaceconnector.transfer.core.edr;

import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.CONTRACT_ID_CLAM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProxyEndpointDataReferenceTransformerTest {

    private static final String DATA_PLANE_ENDPOINT = "http://dataplane.com/api/data";

    private ProxyEndpointDataReferenceTransformer transformer;
    private TokenGenerationService tokenGenerationService;
    private TypeManager typeManager;

    @BeforeEach
    public void setUp() {
        tokenGenerationService = mock(TokenGenerationService.class);
        typeManager = new TypeManager();
        transformer = new ProxyEndpointDataReferenceTransformer(tokenGenerationService, DATA_PLANE_ENDPOINT, typeManager);
    }

    @Test
    void transformationSuccessful() {
        var edr = createEndpointDataReference();
        var generatedToken = TokenRepresentation.Builder.newInstance().token(UUID.randomUUID().toString()).build();
        var claimsCapture = ArgumentCaptor.forClass(Map.class);

        when(tokenGenerationService.generate(claimsCapture.capture())).thenReturn(Result.success(generatedToken));

        var result = transformer.execute(edr);

        verify(tokenGenerationService).generate(claimsCapture.capture());
        assertThat(claimsCapture.getValue())
                .containsOnlyKeys(DATA_ADDRESS_CLAIM, CONTRACT_ID_CLAM, "exp");
        assertThat(claimsCapture.getValue()).hasEntrySatisfying(CONTRACT_ID_CLAM, o -> {
            assertThat(o).isInstanceOf(String.class)
                    .isEqualTo(edr.getContractId());
        });
        assertThat(claimsCapture.getValue()).hasEntrySatisfying(DATA_ADDRESS_CLAIM, o -> {
            assertThat(o).isInstanceOf(String.class);
        });

        var da = typeManager.readValue(claimsCapture.getValue().get(DATA_ADDRESS_CLAIM).toString(), DataAddress.class);
        assertThat(da.getType()).isEqualTo("HttpData");
        assertThat(da.getProperties())
                .containsEntry("endpoint", edr.getAddress())
                .containsEntry("authKey", edr.getAuthKey())
                .containsEntry("authCode", edr.getAuthCode());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent())
                .satisfies(e -> {
                    assertThat(e.getCorrelationId()).isEqualTo(edr.getCorrelationId());
                    assertThat(e.getAddress()).isEqualTo(DATA_PLANE_ENDPOINT);
                    assertThat(e.getAuthKey()).isEqualTo(edr.getAuthKey());
                    assertThat(e.getAuthCode()).isEqualTo(generatedToken.getToken());
                    assertThat(e.getContractId()).isEqualTo(edr.getContractId());
                    assertThat(e.getExpirationEpochSeconds()).isEqualTo(edr.getExpirationEpochSeconds());
                });
    }

    @Test
    void transformationFailure() {
        var edr = createEndpointDataReference();
        when(tokenGenerationService.generate(any())).thenReturn(Result.failure("error"));

        var result = transformer.execute(edr);

        assertThat(result.failed()).isTrue();
    }

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .address("http://example.com")
                .authKey("Api-Key")
                .authCode(UUID.randomUUID().toString())
                .correlationId("correlation-test")
                .contractId(UUID.randomUUID().toString())
                .expirationEpochSeconds(Instant.now().plusSeconds(100).getEpochSecond())
                .build();
    }
}