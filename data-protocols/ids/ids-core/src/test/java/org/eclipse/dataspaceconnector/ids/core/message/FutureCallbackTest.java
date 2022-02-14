package org.eclipse.dataspaceconnector.ids.core.message;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FutureCallbackTest {

    @Test
    void shouldFailFutureIfHandlerThrowsException() {
        var future = new CompletableFuture<String>();
        var callback = new FutureCallback<>(future, response -> {
            throw new RuntimeException("error");
        });

        callback.onResponse(mock(Call.class), dummyResponse());

        assertThat(future).failsWithin(1, TimeUnit.SECONDS);
    }

    private Response dummyResponse() {
        var request = new Request.Builder().url("http://localhost").build();

        return new Response.Builder().request(request).protocol(HTTP_1_1).message("any").code(200).build();
    }
}