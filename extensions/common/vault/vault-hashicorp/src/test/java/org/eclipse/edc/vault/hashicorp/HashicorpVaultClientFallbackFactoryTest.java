package org.eclipse.edc.vault.hashicorp;

import okhttp3.Request;
import org.eclipse.edc.spi.http.FallbackFactories;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mockStatic;

class HashicorpVaultClientFallbackFactoryTest {

    private static final int[] NON_RETRYABLE_STATUS_CODES = {200, 204, 400, 403, 404, 405};

    @Test
    void create_shouldInitializeWithCorrectStatusCodes() {
        try (var mockedFallbackFactories = mockStatic(FallbackFactories.class)) {
            mockedFallbackFactories.when(() -> FallbackFactories.retryWhenStatusIsNotIn(NON_RETRYABLE_STATUS_CODES)).thenCallRealMethod();

            new HashicorpVaultClientFallbackFactory().create(new Request.Builder().url("http://test.local").get().build());

            mockedFallbackFactories.verify(() -> FallbackFactories.retryWhenStatusIsNotIn(NON_RETRYABLE_STATUS_CODES));
        }
    }

}
