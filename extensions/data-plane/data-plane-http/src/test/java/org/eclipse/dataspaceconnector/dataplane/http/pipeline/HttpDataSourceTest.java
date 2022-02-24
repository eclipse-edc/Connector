package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Okio;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.testOkHttpClient;
import static org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpDataSourceTest.CustomInterceptor.JSON_RESPONSE;
import static org.mockito.Mockito.mock;

class HttpDataSourceTest {

    private static final String TEST_ENDPOINT = "http://example.com";

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("providesInvalidSourceBuilders")
    void verifyBuilderThrowsIfInvalidInput(String name, HttpDataSource.Builder builder) {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(builder::build);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void verifyRequest_nameBlankOrNull(String name) {
        var interceptor = new CustomInterceptor();
        var source = defaultBuilder(interceptor).method("GET").name(name).build();

        source.openPartStream();

        var requests = interceptor.getRequests();
        assertThat(requests)
                .hasSize(1)
                .allSatisfy(request -> {
                    assertThat(request.url()).hasToString(TEST_ENDPOINT + "/");
                    assertThat(request.method()).isEqualTo("GET");
                });
    }


    @ParameterizedTest
    @NullAndEmptySource
    void verifyRequest_queryParamsBlankOrNull(String queryParams) {
        var interceptor = new CustomInterceptor();
        var source = defaultBuilder(interceptor).method("GET").name("testfile").queryParams(queryParams).build();

        source.openPartStream();

        var requests = interceptor.getRequests();
        assertThat(requests)
                .hasSize(1)
                .allSatisfy(request -> {
                    assertThat(request.url()).hasToString(TEST_ENDPOINT + "/testfile");
                    assertThat(request.method()).isEqualTo("GET");
                });
    }

    @Test
    void verifyRequest_withNameAndQueryParams() {
        var interceptor = new CustomInterceptor();
        var source = defaultBuilder(interceptor).method("GET").name("testfile").queryParams("foo=bar&hello=world").build();

        source.openPartStream();

        var requests = interceptor.getRequests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0)).satisfies(request -> {
            assertThat(request.url()).hasToString(TEST_ENDPOINT + "/testfile?foo=bar&hello=world");
            assertThat(request.method()).isEqualTo("GET");
        });
    }

    @Test
    void verifyRequest_body() {
        var interceptor = new CustomInterceptor();
        var json = "{ \"foo\" : \"bar\" }";
        var requestBody = RequestBody.create(json, MediaType.get("application/json"));
        var source = defaultBuilder(interceptor).method("POST").requestBody(requestBody).build();

        source.openPartStream();

        var requests = interceptor.getRequests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0)).satisfies(request -> {
            assertThat(request.url()).hasToString(TEST_ENDPOINT + "/");
            assertThat(request.method()).isEqualTo("POST");
            try {
                // check request body
                var sink = Okio.sink(new ByteArrayOutputStream());
                var bufferedSink = Okio.buffer(sink);
                Objects.requireNonNull(request.body()).writeTo(bufferedSink);
                assertThat(bufferedSink.getBuffer().readUtf8()).isEqualTo(json);
            } catch (IOException e) {
                throw new AssertionError("Failed to write body");
            }
        });
    }

    @Test
    void verifyOpenPartStream() {
        var source = defaultBuilder(new CustomInterceptor()).method("GET").build();

        var parts = source.openPartStream().collect(Collectors.toList());

        assertThat(parts).hasSize(1)
                .allSatisfy(part -> {
                    try {
                        assertThat(part.size()).isEqualTo(JSON_RESPONSE.length());
                        assertThat(part.openStream().readAllBytes()).isEqualTo(JSON_RESPONSE.getBytes());
                    } catch (IOException e) {
                        throw new AssertionError("Failed to open Part stream");
                    }
                });
    }

    /**
     * Serves some invalid {@link HttpDataSource.Builder} which are expected to throw exception when calling build() method.
     */
    private static Stream<Arguments> providesInvalidSourceBuilders() {
        var nullHeaderKey = defaultBuilder().header(null, "bar");
        var nullHeaderValue = defaultBuilder().header("foo", null);
        return Stream.of(
                Arguments.of("NULL HEADER KEY", nullHeaderKey),
                Arguments.of("NULL HEADER VALUE", nullHeaderValue)
        );
    }

    private static HttpDataSource.Builder defaultBuilder() {
        return defaultBuilder(new CustomInterceptor());
    }

    private static HttpDataSource.Builder defaultBuilder(Interceptor interceptor) {
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

        public List<Request> getRequests() {
            return requests;
        }
    }
}