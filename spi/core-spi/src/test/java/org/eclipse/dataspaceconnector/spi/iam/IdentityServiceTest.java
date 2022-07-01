package org.eclipse.dataspaceconnector.spi.iam;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class IdentityServiceTest {

    private class IdentityServiceSuccess implements IdentityService {

        @Override
        public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
            return Result.success(TokenRepresentation.Builder.newInstance().token("token").build());
        }

        @Override
        public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {
            return Result.success(ClaimToken.Builder.newInstance().claim("key", "value").build());
        }
    }

    private class IdentityServiceFail implements IdentityService {

        @Override
        public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
            return Result.failure("failed");
        }

        @Override
        public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {
            return Result.failure("failed");
        }
    }

    @Test
    void shouldGetValidTokenIfSucceeds() {
        var success = new IdentityServiceSuccess().obtainClientCredentials(
                TokenParameters.Builder.newInstance().audience("aud").scope("scope").build(), Map.of("key1", "val1"));

        assertThat(success.failed()).isFalse();
        assertThat(success.getContent().getToken()).isEqualTo("token");
        assertThat(success.getContent().getAdditional()).hasSize(1);
        assertThat(success.getContent().getAdditional()).containsEntry("key1", "val1");
    }

    @Test
    void shouldFailIfObtainFails() {
        var fail = new IdentityServiceFail().obtainClientCredentials(
                TokenParameters.Builder.newInstance().audience("aud").scope("scope").build(), Map.of("key1", "val1"));

        assertThat(fail.failed()).isTrue();
        assertThat(fail.getContent()).isNull();
    }
}