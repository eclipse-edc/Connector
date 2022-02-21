package com.siemens.mindsphere.datalake.edc.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class DataLakeAuthInterceptorTest {



    @Test
    void intercept() throws IOException {

        final ObjectMapper objectMapper = new ObjectMapper();

        final TechnicalUserTokenResponseDto technicalUserTokenResponseDto = new TechnicalUserTokenResponseDto();
        technicalUserTokenResponseDto.setAccessToken("access_token");

        MockWebServer targetServer = new MockWebServer();
        targetServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(technicalUserTokenResponseDto)));
        targetServer.start(9991);

        MockWebServer authServer = new MockWebServer();
        authServer.enqueue(new MockResponse().setBody("not important for the test"));
        authServer.start(9992);


        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new DataLakeAuthInterceptor())
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new DataLakeAuthInterceptor())
                .build();

    }
}