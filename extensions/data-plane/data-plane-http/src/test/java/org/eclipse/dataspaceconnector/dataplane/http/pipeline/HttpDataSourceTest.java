package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Okio;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.testOkHttpClient;
import static org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpDataSourceTest.CustomInterceptor.JSON_RESPONSE;
import static org.mockito.Mockito.mock;

class HttpDataSourceTest {

    private static final String TEST_ENDPOINT = "http://example.com";

    private CustomInterceptor interceptor;

    @BeforeEach
    public void setUp() {
        interceptor = new CustomInterceptor();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void verifyRequest_nameBlankOrNull(String name) {
        var source = defaultBuilder().method("GET").name(name).build();

        source.openPartStream();

        var request = interceptor.getInterceptedRequest();
        assertThat(request.url()).hasToString(TEST_ENDPOINT + "/");
        assertThat(request.method()).isEqualTo("GET");
    }


    @ParameterizedTest
    @NullAndEmptySource
    void verifyRequest_queryParamsBlankOrNull(String queryParams) {
        var source = defaultBuilder().method("GET").name("testfile").queryParams(queryParams).build();

        source.openPartStream();

        var request = interceptor.getInterceptedRequest();
        assertThat(request.url()).hasToString(TEST_ENDPOINT + "/testfile");
        assertThat(request.method()).isEqualTo("GET");
    }

    @Test
    void verifyRequest_withNameAndQueryParams() {
        var source = defaultBuilder().method("GET").name("testfile").queryParams("foo=bar&hello=world").build();

        source.openPartStream();

        var request = interceptor.getInterceptedRequest();
        assertThat(request.url()).hasToString(TEST_ENDPOINT + "/testfile?foo=bar&hello=world");
        assertThat(request.method()).isEqualTo("GET");
    }

    @Test
    void verifyRequest_body() {
        var json = "{ \"foo\" : \"bar\" }";
        var source = defaultBuilder().method("POST").requestBody(MediaType.get("application/json"), json).build();

        source.openPartStream();

        var request = interceptor.getInterceptedRequest();
        assertThat(request.url()).hasToString(TEST_ENDPOINT + "/");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(extractRequestBody(request)).isEqualTo(json);
    }

    @Test
    void verifyOpenPartStream() {
        var source = defaultBuilder().method("GET").build();

        var parts = source.openPartStream().collect(Collectors.toList());

        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).openStream()).hasContent(JSON_RESPONSE);
    }

    private static String extractRequestBody(Request request) {
        try {
            var sink = Okio.sink(new ByteArrayOutputStream());
            var bufferedSink = Okio.buffer(sink);
            Objects.requireNonNull(request.body()).writeTo(bufferedSink);
            return bufferedSink.getBuffer().readUtf8();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private HttpDataSource.Builder defaultBuilder() {
        var monitor = mock(Monitor.class);
        var retryPolicy = new RetryPolicy<>().withMaxAttempts(1);
        var httpClient = testOkHttpClient().newBuilder().addInterceptor(interceptor).build();
        return HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .monitor(monitor)
                .sourceUrl(TEST_ENDPOINT)
                .requestId(UUID.randomUUID().toString())
                .retryPolicy(retryPolicy);
    }

    static final class CustomInterceptor implements Interceptor {

        public static final String JSON_RESPONSE = "{\"hello\" : \"world\"}";

        private final List<Request> requests = new ArrayList<>();

        @NotNull
        @Override
        public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
            requests.add(chain.request());
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(HTTP_1_1).code(200)
                    .body(ResponseBody.create(JSON_RESPONSE, MediaType.get("application/json"))).message("ok")
                    .build();
        }

        public Request getInterceptedRequest() {
            return requests.stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No request intercepted"));
        }
    }
}