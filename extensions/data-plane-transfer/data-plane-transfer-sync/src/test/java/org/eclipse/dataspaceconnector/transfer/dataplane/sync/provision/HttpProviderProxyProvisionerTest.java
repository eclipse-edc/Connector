package org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.CONTRACT_ID_CLAIM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema.EXPIRATION;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema.TOKEN;
import static org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpProviderProxyProvisionerTest {

    private static final long TOKEN_VALIDITY_TIME = 100;
    private static final String DATA_PROXY_ADDRESS = "http://example.com";

    private DataAddressResolver dataAddressResolverMock;
    private TokenGenerationService tokenGenerationServiceMock;
    private HttpProviderProxyProvisioner provisioner;
    private DataEncrypter encrypterMock;
    private TypeManager typeManager;

    @BeforeEach
    void setUp() {
        dataAddressResolverMock = mock(DataAddressResolver.class);
        tokenGenerationServiceMock = mock(TokenGenerationService.class);
        typeManager = new TypeManager();
        encrypterMock = mock(DataEncrypter.class);
        provisioner = new HttpProviderProxyProvisioner(DATA_PROXY_ADDRESS, dataAddressResolverMock, encrypterMock, tokenGenerationServiceMock, TOKEN_VALIDITY_TIME, typeManager);
    }

    @Test
    void verifyCanProvision_success() {
        var rd = createResourceDefinition(TYPE);

        var result = provisioner.canProvision(rd);

        assertThat(result).isTrue();
    }

    @Test
    void verifyCanProvision_typeNotSupported() {
        var rd = createResourceDefinition("dummy");

        var result = provisioner.canProvision(rd);

        assertThat(result).isFalse();
    }

    @Test
    void verifyProvision_success() {
        var rd = createResourceDefinition(TYPE);
        var address = DataAddress.Builder.newInstance().type("test").build();
        var token = TokenRepresentation.Builder.newInstance().token(UUID.randomUUID().toString()).build();
        var claimsCaptor = ArgumentCaptor.forClass(JWTClaimsSet.class);
        when(dataAddressResolverMock.resolveForAsset(rd.getAssetId())).thenReturn(address);
        when(tokenGenerationServiceMock.generate(any(JWTClaimsSet.class))).thenReturn(Result.success(token));
        when(encrypterMock.encrypt(typeManager.writeValueAsString(address))).thenReturn("encrypted-data-address");

        var future = provisioner.provision(rd);

        verify(tokenGenerationServiceMock, times(1)).generate(claimsCaptor.capture());

        assertThat(claimsCaptor.getValue()).satisfies(claimsSet -> {
            assertThat(claimsSet.getExpirationTime()).isNotNull().isAfter(Instant.now());
            assertThat(claimsSet.getClaim(CONTRACT_ID_CLAIM)).hasToString(rd.getContractId());
            assertThat(claimsSet.getClaim(DATA_ADDRESS_CLAIM)).hasToString("encrypted-data-address");
        });

        assertThat(future).succeedsWithin(500L, TimeUnit.MILLISECONDS)
                .satisfies(response -> {
                    var resource = response.getResource();
                    assertThat(resource).isInstanceOf(HttpProviderProxyProvisionedResource.class);
                    var httpProvisionedResource = (HttpProviderProxyProvisionedResource) resource;
                    assertThat(httpProvisionedResource.createDataDestination()).satisfies(destDataAddress -> {
                        assertThat(destDataAddress.getType()).isEqualTo(TYPE);
                        assertThat(destDataAddress.getProperty(ENDPOINT)).isEqualTo(DATA_PROXY_ADDRESS);
                        assertThat(destDataAddress.getProperty(TOKEN)).isEqualTo(token.getToken());
                        var expiration = Instant.ofEpochSecond(Long.parseLong(destDataAddress.getProperty(EXPIRATION)));
                        assertThat(Instant.now().plusSeconds(TOKEN_VALIDITY_TIME)).isCloseTo(expiration, byLessThan(5, ChronoUnit.SECONDS));
                    });
                });
    }

    @Test
    void verifyCanDeprovision() {
        assertThat(provisioner.deprovision(null)).succeedsWithin(500L, TimeUnit.MILLISECONDS);
    }

    @Test
    void verifyDeprovision() {
        assertThat(provisioner.canDeprovision(null)).isFalse();
    }

    private static HttpProviderProxyResourceDefinition createResourceDefinition(String dataRequestType) {
        return HttpProviderProxyResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .transferProcessId("tp-test")
                .type(dataRequestType)
                .assetId("asset-test")
                .contractId("contract-test")
                .build();
    }
}