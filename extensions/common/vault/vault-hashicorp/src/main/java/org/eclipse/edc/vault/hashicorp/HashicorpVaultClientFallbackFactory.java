package org.eclipse.edc.vault.hashicorp;

import dev.failsafe.Fallback;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.spi.http.FallbackFactory;

import static org.eclipse.edc.spi.http.FallbackFactories.retryWhenStatusIsNotIn;

/**
 * Implements a {@link Fallback}factory for requests executed against the Hashicorp Vault.
 *
 *  @see <a href="https://developer.hashicorp.com/vault/api-docs">Hashicorp Vault Api</a> for more information on
 *  retryable error codes.
 */
public class HashicorpVaultClientFallbackFactory implements FallbackFactory {

    private static final int[] NON_RETRYABLE_STATUS_CODES = {200, 204, 400, 403, 404, 405};

    @Override
    public Fallback<Response> create(Request request) {
        return retryWhenStatusIsNotIn(NON_RETRYABLE_STATUS_CODES).create(request);
    }
}
