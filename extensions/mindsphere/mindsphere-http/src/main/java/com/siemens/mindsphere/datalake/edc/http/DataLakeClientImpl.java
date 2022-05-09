package com.siemens.mindsphere.datalake.edc.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class DataLakeClientImpl implements DataLakeClient {
    public DataLakeClientImpl(URI dataLakeBaseUrl, OkHttpClient client, ObjectMapper objectMapper) {
        this.dataLakeBaseUrl = dataLakeBaseUrl;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public DataLakeClientImpl(URI dataLakeBaseUrl) {
        this(dataLakeBaseUrl, new OkHttpClient(), new ObjectMapper());
    }

    public DataLakeClientImpl(OauthClientDetails oauthClientDetails, URI dataLakeBaseUrl) {
        this(dataLakeBaseUrl, new OkHttpClient(), new ObjectMapper());
        this.oauthClientDetails = oauthClientDetails;
    }

    public static void setInstance(DataLakeClientImpl clientImpl) {
        instance = clientImpl;
    }

    public static DataLakeClientImpl getInstance() {
        return instance;
    }

    private static DataLakeClientImpl instance;


    private OauthClientDetails oauthClientDetails;

    private final URI dataLakeBaseUrl;

    private final OkHttpClient client;

    private final ObjectMapper objectMapper;

    private static final String DATA_LAKE_SIGN_REQ_URL = "/generateUploadObjectUrls";

    private static final String TENANT_IDENTIFIER = "ten=";
    
    private static final String X_SPACE_AUTH_KEY = "X-SPACE-AUTH-KEY";


    @Override
    public URL getUrl(String path) throws DataLakeException {
        try {
            String extractedTenant = extractTenantFromPath(path);
            if (extractedTenant == null) {
                throw new DataLakeException("Could not identify tenant");
            }
            String accessToken = getAccessToken(extractedTenant);
            System.out.println("Access token: " + accessToken);

            final SignUrlRequestContainerDto requestContainerDto = SignUrlRequestContainerDto.composeForSinglePath(path);
            final String payloadString = objectMapper.writeValueAsString(requestContainerDto);
            final RequestBody requestPayload = RequestBody.create(payloadString, MediaType.parse("application/json"));
            final Request request = new Request.Builder()
                    .method("POST", requestPayload)
                    .url(dataLakeBaseUrl.resolve(DATA_LAKE_SIGN_REQ_URL).toURL())
                    .build();
            final Response response = client.newCall(request).execute();
            System.out.println("Response code: " + response.code());
            System.out.println("Response message: " + response.message());

            if (!response.isSuccessful()) {
                throw new DataLakeException("Request to DataLake was not successful");
            }

            final SignUrlResponseContainerDto signUrlResponseContainerDto = objectMapper.readValue(response.body()
                    .bytes(), SignUrlResponseContainerDto.class);

            final SignUrlResponseContainerDto.SignUrlResponseDto signedUrl = signUrlResponseContainerDto.getObjectUrls().stream().findFirst().orElseThrow(() -> new DataLakeException("No such path in Data Lake"));

            return new URL(signedUrl.getSignedUrl());
        } catch (IOException e) {
            e.printStackTrace();
            throw new DataLakeException("Error getting signed URL", e);
        }
    }

    private String extractTenantFromPath(String path) {
        if (path == null || !path.contains(TENANT_IDENTIFIER)) {
            return null;
        }
        int tenantStartIndex = path.indexOf(TENANT_IDENTIFIER);
        int firstSlashAfterTenantIndex = path.indexOf("/", tenantStartIndex);

        if (firstSlashAfterTenantIndex == -1) {
            return null;
        }
        return path.substring(tenantStartIndex + TENANT_IDENTIFIER.length(), firstSlashAfterTenantIndex);
    }

    private String getAccessToken(String tenant) throws IOException {
        TechnicalUserTokenRequestDto technicalUserTokenRequestDto = new TechnicalUserTokenRequestDto(oauthClientDetails.getTenant(),
        tenant, oauthClientDetails.getClientAppName(), oauthClientDetails.getClientAppVersion());
        
        final String requestPayload = objectMapper.writeValueAsString(technicalUserTokenRequestDto);
        final RequestBody requestBody = RequestBody.create(requestPayload, MediaType.parse("application/json"));

        System.out.println("Credentials: " + oauthClientDetails.getBase64Credentials() );
        final Request tokenRequest = new Request.Builder().url(oauthClientDetails.getAccessTokenUrl())
        .method("POST", requestBody)
        .header(X_SPACE_AUTH_KEY, String.format("Bearer %s", oauthClientDetails.getBase64Credentials()))
        .build();
         
        final Call call = client.newCall(tokenRequest);
        final Response response = call.execute();
        System.out.println("Response access token: " + response.code());

        final TechnicalUserTokenResponseDto technicalUserTokenResponseDto = objectMapper.readValue(response.body()
                .bytes(), TechnicalUserTokenResponseDto.class);
    
        return "Bearer " + technicalUserTokenResponseDto.getAccessToken();
    }

    @Override
    public boolean isPresent(String path) {
        throw new UnsupportedOperationException("TODO");
    }
}
