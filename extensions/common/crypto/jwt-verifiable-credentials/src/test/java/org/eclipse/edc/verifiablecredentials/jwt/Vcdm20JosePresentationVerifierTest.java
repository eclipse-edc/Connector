package org.eclipse.edc.verifiablecredentials.jwt;

import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Vcdm20JosePresentationVerifierTest {
    private final TokenValidationService tokenValidationService = mock();
    private final PublicKeyResolver publicKeyResolver = mock();
    private final Vcdm20JosePresentationVerifier verifier = new Vcdm20JosePresentationVerifier(tokenValidationService, publicKeyResolver);

    @BeforeEach
    void setUp() {
        when(tokenValidationService.validate(anyString(), any())).thenReturn(Result.success(
                ClaimToken.Builder.newInstance().build()
        ));
    }
    @ParameterizedTest
    @ValueSource(strings = { TestConstants.VP_SIMPLE_JOSE_ENVELOPED_CREDENTIAL, TestConstants.VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL })
    void canHandle(String rawJose) {
        assertThat(verifier.canHandle(rawJose)).isTrue();
    }

    @Test
    void canHandle_vcdm11() {
        assertThat(verifier.canHandle(TestConstants.VP_EXAMPLE_VCDM11)).isFalse();
    }

    @Test
    void canHandle_notVpToken() {
        assertThat(verifier.canHandle(TestConstants.RANDOM_JWT)).isFalse();
    }

    @Test
    void canHandle_notJwt() {
        assertThat(verifier.canHandle("not a jwt")).isFalse();
    }


    @Test
    void verify_enveloped() {
        assertThat(verifier.verify(TestConstants.VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL, mock())).isSucceeded();
    }

    @Test
    void verify_simple() {
        assertThat(verifier.verify(TestConstants.VP_SIMPLE_JOSE_ENVELOPED_CREDENTIAL, mock())).isSucceeded();

    }

    @Test
    void verify_randomJwt_expectFailure() {
        assertThat(verifier.verify(TestConstants.RANDOM_JWT, mock())).isFailed()
                .detail().contains("Not a valid VP token - missing the 'type' claim");
    }

    @Test
    void verify_tokenValidationFailed_expectFailure() {
        when(tokenValidationService.validate(anyString(), any())).thenReturn(Result.failure("test-failure"));
        assertThat(verifier.verify(TestConstants.VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL, mock())).isFailed()
                .detail().contains("test-failure");
    }

    @ParameterizedTest
    @ValueSource(strings = { TestConstants.VP_SIMPLE_JOSE_ENVELOPED_CREDENTIAL, TestConstants.VP_ENVELOPED_JOSE_ENVELOPED_CREDENTIAL })
    void verify_multipleVC_oneInvalid(String vpToken){
        when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class)))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build())) // VP token
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build())) // first VC
                .thenReturn(Result.failure("test-failure")); // second VC

        assertThat(verifier.verify(vpToken, mock())).isFailed()
                .detail().contains("test-failure");
    }
}
