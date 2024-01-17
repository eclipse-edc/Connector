package org.eclipse.edc.vault.hashicorp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_RETRY_BACKOFF_BASE_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER_DEFAULT;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_TTL_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HashicorpVaultConfigValuesTest {

    private static final String TOKEN = "token";
    private static final String URL = "URL";
    private static final String HEALTH_CHECK_PATH = "/healthcheck/path";
    private static final String SECRET_PATH = "/secret/path";

    @Test
    void createConfigValues_withDefaultValues_shouldSucceed() {
        var configValues = assertDoesNotThrow(() -> createConfigValues(
                URL,
                VAULT_RETRY_BACKOFF_BASE_DEFAULT,
                TOKEN,
                VAULT_TOKEN_TTL_DEFAULT,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(configValues.url()).isEqualTo(URL);
        assertThat(configValues.healthCheckEnabled()).isEqualTo(true);
        assertThat(configValues.healthCheckPath()).isEqualTo(HEALTH_CHECK_PATH);
        assertThat(configValues.healthStandbyOk()).isEqualTo(true);
        assertThat(configValues.retryBackoffBase()).isEqualTo(VAULT_RETRY_BACKOFF_BASE_DEFAULT);
        assertThat(configValues.token()).isEqualTo(TOKEN);
        assertThat(configValues.ttl()).isEqualTo(VAULT_TOKEN_TTL_DEFAULT);
        assertThat(configValues.renewBuffer()).isEqualTo(VAULT_TOKEN_RENEW_BUFFER_DEFAULT);
        assertThat(configValues.secretPath()).isEqualTo(SECRET_PATH);
    }

    @Test
    void createConfigValues_withVaultUrlNull_shouldThrowException() {
        var throwable = assertThrows(Exception.class, () -> createConfigValues(
                null,
                VAULT_RETRY_BACKOFF_BASE_DEFAULT,
                TOKEN,
                VAULT_TOKEN_TTL_DEFAULT,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(throwable.getMessage()).isEqualTo("Vault url must not be null");
    }

    @Test
    void createConfigValues_withVaultTokenNull_shouldThrowException() {
        var throwable = assertThrows(Exception.class, () -> createConfigValues(
                URL,
                VAULT_RETRY_BACKOFF_BASE_DEFAULT,
                null,
                VAULT_TOKEN_TTL_DEFAULT,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(throwable.getMessage()).isEqualTo("Vault token must not be null");
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 0.9})
    void createConfigValues_whenVaultRetryBackoffBaseNotGreaterThan1_shouldThrowException(double value) {
        var throwable = assertThrows(Exception.class, () -> createConfigValues(
                URL,
                value,
                TOKEN,
                VAULT_TOKEN_TTL_DEFAULT,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(throwable.getMessage()).isEqualTo("Vault retry exponential backoff base be greater than 1");
    }

    @Test
    void createConfigValues_withVaultTokenTtlLessThan5_shouldThrowException() {
        var throwable = assertThrows(Exception.class, () -> createConfigValues(
                URL,
                VAULT_RETRY_BACKOFF_BASE_DEFAULT,
                TOKEN,
                4,
                VAULT_TOKEN_RENEW_BUFFER_DEFAULT));
        assertThat(throwable.getMessage()).isEqualTo("Vault token ttl minimum value is 5");
    }

    @ParameterizedTest
    @ValueSource(longs = {VAULT_TOKEN_TTL_DEFAULT, VAULT_TOKEN_TTL_DEFAULT + 1})
    void createConfigValues_whenVaultTokenRenewBufferLessThan5_shouldThrowException(long value) {
        var throwable = assertThrows(Exception.class, () -> createConfigValues(
                URL,
                VAULT_RETRY_BACKOFF_BASE_DEFAULT,
                TOKEN,
                VAULT_TOKEN_TTL_DEFAULT,
                value));
        assertThat(throwable.getMessage()).isEqualTo("Vault token renew buffer value must be less than ttl value");
    }

    private HashicorpVaultConfigValues createConfigValues(String url,
                                                          double retryBackoffBase,
                                                          String token,
                                                          long ttl,
                                                          long renewBuffer) {
        return HashicorpVaultConfigValues.Builder.newInstance()
                .url(url)
                .healthCheckEnabled(true)
                .healthCheckPath(HEALTH_CHECK_PATH)
                .healthStandbyOk(true)
                .retryBackoffBase(retryBackoffBase)
                .token(token)
                .ttl(ttl)
                .renewBuffer(renewBuffer)
                .secretPath(SECRET_PATH)
                .build();
    }
}
