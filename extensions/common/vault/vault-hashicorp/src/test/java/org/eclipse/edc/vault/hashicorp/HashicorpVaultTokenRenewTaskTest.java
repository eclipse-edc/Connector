package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpData;
import org.eclipse.edc.vault.hashicorp.model.TokenLookUpResponse;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewAuth;
import org.eclipse.edc.vault.hashicorp.model.TokenRenewResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class HashicorpVaultTokenRenewTaskTest {
    private static final long VAULT_TOKEN_TTL = 5L;
    private static final long RENEW_BUFFER = 5L;
    private final Monitor monitor = mock();
    private final HashicorpVaultClient vaultClient = mock();
    private final HashicorpVaultTokenRenewTask tokenRenewTask = new HashicorpVaultTokenRenewTask(
            ExecutorInstrumentation.noop(),
            vaultClient,
            RENEW_BUFFER,
            monitor
    );

    @Test
    void start_withValidAndRenewableToken_shouldScheduleNextTokenRenewal() {
        var tokenLookUpResponse = TokenLookUpResponse.Builder.newInstance()
                .data(TokenLookUpData.Builder.newInstance()
                        .ttl(2L)
                        .renewable(true)
                        .build())
                .build();
        doReturn(Result.success(tokenLookUpResponse)).when(vaultClient).lookUpToken();
        var tokenRenewResponse = TokenRenewResponse.Builder.newInstance()
                .auth(TokenRenewAuth.Builder.newInstance()
                        .ttl(2L)
                        .build())
                .build();

        // return a successful renewal result twice
        // first result should be consumed by the initial token renewal
        // second renewal should be consumed by the first renewal iteration
        doReturn(Result.success(tokenRenewResponse))
                .doReturn(Result.success(tokenRenewResponse))
                // break the renewal loop by returning a failed renewal result on the 3rd attempt
                .doReturn(Result.failure("break the loop"))
                .when(vaultClient)
                .renewToken();

        tokenRenewTask.start();

        await()
                .atMost(VAULT_TOKEN_TTL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor, never()).warning(matches("Initial token look up failed with reason: *"));
                    verify(monitor, never()).warning(matches("Initial token renewal failed with reason: *"));
                    // initial token look up
                    verify(vaultClient).lookUpToken();
                    // initial renewal + first scheduled renewal + second scheduled renewal
                    verify(vaultClient, times(3)).renewToken();
                    verify(monitor).warning("Scheduled token renewal failed: break the loop");
                });
    }

    @Test
    void start_withFailedTokenLookUp_shouldNotScheduleNextTokenRenewal() {
        doReturn(Result.failure("Token look up failed with status 403")).when(vaultClient).lookUpToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor).warning("Initial token look up failed with reason: Token look up failed with status 403");
                    verify(vaultClient, never()).renewToken();
                });
    }

    @Test
    void start_withTokenNotRenewable_shouldNotScheduleNextTokenRenewal() {
        var tokenLookUpResponse = TokenLookUpResponse.Builder.newInstance()
                .data(TokenLookUpData.Builder.newInstance()
                        .ttl(2L)
                        .renewable(false)
                        .build())
                .build();
        doReturn(Result.success(tokenLookUpResponse)).when(vaultClient).lookUpToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(vaultClient, never()).renewToken();
                });
    }

    @Test
    void start_withFailedTokenRenew_shouldNotScheduleNextTokenRenewal() {
        var tokenLookUpResponse = TokenLookUpResponse.Builder.newInstance()
                .data(TokenLookUpData.Builder.newInstance()
                        .ttl(2L)
                        .renewable(true)
                        .build())
                .build();
        doReturn(Result.success(tokenLookUpResponse)).when(vaultClient).lookUpToken();
        doReturn(Result.failure("Token renew failed with status: 403")).when(vaultClient).renewToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor).warning("Initial token renewal failed with reason: Token renew failed with status: 403");
                    verify(vaultClient, atMostOnce()).renewToken();
                });
    }
}
