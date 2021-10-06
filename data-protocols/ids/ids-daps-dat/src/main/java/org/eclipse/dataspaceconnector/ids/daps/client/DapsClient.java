package org.eclipse.dataspaceconnector.ids.daps.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.ids.daps.Dat;
import org.eclipse.dataspaceconnector.ids.daps.sec.PrivateKeyProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class DapsClient {
    private static final String GRANT_TYPE = "grant_type";
    private static final String GRANT_TYPE_VALUE = "client_credentials";
    private static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    private static final String CLIENT_ASSERTION_TYPE_VALUE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String CLIENT_ASSERTION = "client_assertion";
    private static final String SCOPE = "scope";
    private static final String SCOPE_VALUE = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL";
    private static final String CLAIM_CONTEXT = "@context";
    private static final String CLAIM_CONTEXT_VALUE = "https://w3id.org/idsa/contexts/context.jsonld";
    private static final String CLAIM_TYPE = "@type";
    private static final String CLAIM_TYPE_VALUE = "ids:DatRequestToken";
    private static final String AUDIENCE = "idsc:IDS_CONNECTORS_ALL";
    private static final int JWT_TTL_SECONDS = 60;

    private final URL tokenUrl;
    private final PrivateKeyProvider privateKeyProvider;
    private final DapsIssuer dapsIssuer;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DapsClient(
            final URL tokenUrl,
            final PrivateKeyProvider privateKeyProvider,
            final DapsIssuer dapsIssuer,
            final OkHttpClient httpClient,
            final ObjectMapper objectMapper) {
        Objects.requireNonNull(tokenUrl);
        Objects.requireNonNull(privateKeyProvider);
        Objects.requireNonNull(dapsIssuer);
        Objects.requireNonNull(httpClient);
        Objects.requireNonNull(objectMapper);

        this.tokenUrl = tokenUrl;
        this.privateKeyProvider = privateKeyProvider;
        this.dapsIssuer = dapsIssuer;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    // TODO refactor exception handling
    @NotNull
    public Dat requestDat() throws DapsClientException {
        final String issuer = dapsIssuer.getValue();
        final PrivateKey privateKey = privateKeyProvider.getPrivateKey();
        final String token = createRequestToken(issuer, privateKey);
        final FormBody formBody = createFormBody(token);
        final Request request = new Request.Builder()
                .url(tokenUrl)
                .post(formBody)
                .build();

        final Response response = sendRequest(request);
        final AccessTokenResponse dapsAccessTokenResponse = mapResponse(response);
        final Dat dat = createDat(dapsAccessTokenResponse);

        return dat;
    }

    private String createRequestToken(final String issuer, final PrivateKey privateKey) {
        final Date issuanceDate = Date.from(Instant.now());
        final Date expiryDate = Date.from(Instant.now().plusSeconds(JWT_TTL_SECONDS));

        final JwtBuilder jwtBuilder = Jwts.builder()
                .setIssuer(issuer)
                .setSubject(issuer)
                .claim(CLAIM_CONTEXT, CLAIM_CONTEXT_VALUE)
                .claim(CLAIM_TYPE, CLAIM_TYPE_VALUE)
                .setExpiration(expiryDate)
                .setAudience(AUDIENCE)
                .setIssuedAt(issuanceDate)
                .setNotBefore(expiryDate);

        return jwtBuilder.signWith(privateKey, SignatureAlgorithm.RS256).compact();
    }

    private FormBody createFormBody(final String jws) {
        return new okhttp3.FormBody.Builder()
                .add(GRANT_TYPE, GRANT_TYPE_VALUE)
                .add(CLIENT_ASSERTION_TYPE, CLIENT_ASSERTION_TYPE_VALUE)
                .add(CLIENT_ASSERTION, jws)
                .add(SCOPE, SCOPE_VALUE)
                .build();
    }

    private Response sendRequest(final Request request) throws DapsClientException {
        final Response response;
        try {
            response = httpClient.newCall(request).execute();
        } catch (final IOException ioException) {
            throw new DapsClientException(
                    String.format("Unable to execute http call: %s", ioException.getMessage()),
                    ioException
            );
        }

        final int responseStatusCode = response.code();
        if (responseStatusCode != 200) {
            throw new DapsClientException(
                    String.format("Unexpected response status code received: %s", responseStatusCode)
            );
        }

        return response;
    }

    private AccessTokenResponse mapResponse(final Response response) throws DapsClientException {
        final ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new DapsClientException(
                    "Unexpected empty response payload received"
            );
        }

        final AccessTokenResponse dapsAccessTokenResponse;
        try (final InputStream inputStream = response.body().byteStream()) {
            dapsAccessTokenResponse = objectMapper.readValue(inputStream, AccessTokenResponse.class);
        } catch (final IOException ioException) {
            throw new DapsClientException(
                    String.format("Unexpected json response received: %s", ioException.getMessage()),
                    ioException
            );
        }

        return dapsAccessTokenResponse;
    }

    private Dat createDat(final AccessTokenResponse accessTokenResponse) {
        final String token = accessTokenResponse.getAccessToken();
        final int expiresIn = accessTokenResponse.getExpiresIn();

        // TODO Do we need to pay attention to potential time drifts?
        final Instant expirationDate = Instant.now().plus(Duration.ofSeconds(expiresIn));

        return new Dat(token, expirationDate);
    }
}
