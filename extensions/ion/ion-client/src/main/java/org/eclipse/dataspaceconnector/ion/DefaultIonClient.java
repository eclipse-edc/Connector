package org.eclipse.dataspaceconnector.ion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidResolveResponse;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.ion.spi.request.IonRequest;
import org.eclipse.dataspaceconnector.ion.util.HexStringUtils;
import org.eclipse.dataspaceconnector.ion.util.JsonCanonicalizer;
import org.eclipse.dataspaceconnector.ion.util.SortingNodeFactory;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Standard implementation of the {@link IonClient} interface that can resolve DIDs from ION and submit anchor requests to id.
 */
public class DefaultIonClient implements IonClient {
    private static final String DID_METHOD = "did:ion";

    private static final String DEFAULT_RESOLUTION_ENDPOINT = "https://beta.discover.did.microsoft.com/1.0";
    private static final String IDENTIFIERS_PATH = "/identifiers";
    private static final String OPERATIONS_PATH = "/operations";
    private final String resolutionEndpoint;
    private final ObjectMapper typeManager;

    public DefaultIonClient(ObjectMapper typeManager) {
        this(DEFAULT_RESOLUTION_ENDPOINT, typeManager);
    }

    public DefaultIonClient(String resolutionEndpoint, ObjectMapper typeManager) {
        this.resolutionEndpoint = resolutionEndpoint;
        this.typeManager = typeManager;
    }

    @Override
    public String getMethod() {
        return DID_METHOD;
    }

    @Override
    public DidDocument submit(IonRequest ionRequest) {
        String requestBodyJson;
        try {
            requestBodyJson = typeManager.writeValueAsString(ionRequest);
        } catch (JsonProcessingException ex) {
            throw new IonException(ex);
        }

        var client = getHttpClient();

        try {
            var request = HttpRequest.newBuilder(new URI(resolutionEndpoint + OPERATIONS_PATH))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBodyJson = response.body();
            if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                var didResponse = typeManager.readValue(responseBodyJson, DidResolveResponse.class);
                return didResponse.getDidDocument();
            } else {
                if (response.statusCode() >= 500) {
                    throw new IonRequestException("Unexpected 5xx response " + responseBodyJson);
                } else if (response.statusCode() >= 400) {
                    //means bad request, should retry
                    throw new IonRequestException("Bad Request, please retry with another challenge");
                } else if (response.statusCode() >= 300) {
                    throw new IonRequestException("Unexpected 3xx response: " + response.body());
                }
                throw new IonRequestException("Unexpected response: " + response.statusCode() + ", " + responseBodyJson);
            }

        } catch (Exception ex) {
            throw new IonException(ex);
        }
    }

    @Override
    public DidDocument resolve(String didUri) {
        try {
            var rq = HttpRequest.newBuilder(new URI(resolutionEndpoint + IDENTIFIERS_PATH + "/" + didUri))
                    .GET()
                    .build();

            var response = getHttpClient().send(rq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                var body = response.body();
                DidResolveResponse didResolveResponse = typeManager.readValue(body, DidResolveResponse.class);
                return didResolveResponse.getDidDocument();
            }
            throw new IonRequestException("resolving the DID URI was unsuccessful: code = " + response.statusCode() + " content= " + response.body());
        } catch (Exception ex) {
            throw new IonRequestException(ex);
        }
    }

    public void submitWithChallengeResponse(IonRequest request, String challengeEndpoint, String solutionEndpoint) {
        System.out.println("Getting challenge from " + challengeEndpoint);

        ObjectMapper objectMapper = JsonMapper.builder()
                .nodeFactory(new SortingNodeFactory())
                .build();


        String challengeNonce;
        String largestAllowedHash;
        int validDuration;

        try {
            var rq = HttpRequest.newBuilder(new URI(challengeEndpoint))
                    .GET()
                    .build();

            var response = getHttpClient().send(rq, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() > 299) {
                throw new IonException("Error obtaining challenge: " + response.body());
            }
            var json = Objects.requireNonNull(response.body());
            System.out.println("challenge: " + json);
            var map = objectMapper.readValue(json, Map.class);
            challengeNonce = (String) map.get("challengeNonce");
            largestAllowedHash = (String) map.get("largestAllowedHash");
            validDuration = (int) map.get("validDurationInMinutes") * 60 * 1000;

        } catch (Exception ex) {
            throw new IonException(ex);
        }


        // for Argon2id hashing check here for reference:
        // https://mkyong.com/java/java-password-hashing-with-argon2/
        var startTime = new Date();

        String requestBodyJson = JsonCanonicalizer.canonicalizeAsString(request);

        String answerHashString;
        String answerNonce;
        String answerNonceHex;

        byte[] salt = HexStringUtils.encodeToHexBytes(challengeNonce);


        do {
            Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withIterations(1)
                    .withParallelism(32)
                    .withMemoryAsKB(1000)
                    .withSalt(salt)
                    .build();
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);

            answerNonce = createNonce();
            answerNonceHex = HexStringUtils.encodeToHex(answerNonce);

            String password = answerNonceHex + requestBodyJson;

            int hashLen = 32;
            var answerHash = new byte[hashLen];
            generator.generateBytes(password.getBytes(), answerHash);

            answerHashString = HexStringUtils.bytesToHexString(answerHash);
            System.out.println("Answer Hash:     " + answerHashString);
            System.out.println("Largest Allowed: " + largestAllowedHash);
            System.out.println("***********************************************************");
        } while (HexStringUtils.compareHex(answerHashString, largestAllowedHash) > 0 && new Date().getTime() - startTime.getTime() < validDuration);


        if (new Date().getTime() - startTime.getTime() > validDuration) {
            throw new IonException("Time expired! Finding an acceptable hash took longer than " + validDuration + " seconds");
        }

        try {
            var solutionRequest = HttpRequest.newBuilder(new URI(solutionEndpoint))
                    .header("Challenge-Nonce", challengeNonce)
                    .header("Answer-Nonce", answerNonce)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            var solutionResponse = getHttpClient().send(solutionRequest, HttpResponse.BodyHandlers.ofString());

            if (solutionResponse.statusCode() >= 200 && solutionResponse.statusCode() <= 299) {
                System.out.println("Successfully submitted the anchor request");
                var body = solutionResponse.body();

                System.out.println("Body: " + body);
            } else {
                if (solutionResponse.statusCode() >= 500) {
                    throw new IonRequestException("Unexpected 5xx response " + solutionResponse.body());
                } else if (solutionResponse.statusCode() >= 400) {
                    //means bad request, should retry
                    throw new IonRequestException("Bad Request, please retry with another challenge");
                } else if (solutionResponse.statusCode() >= 300) {
                    throw new IonRequestException("Unexpected 3xx response: " + solutionResponse.body());
                }
            }

        } catch (Exception ex) {
            throw new IonException(ex);
        }
    }

    @NotNull
    private HttpClient getHttpClient() {
        return HttpClient.newBuilder().build();
    }

    private String createNonce() {
        var random = new SecureRandom();

        // size between 250 - 500
        var size = 250 + (int) Math.floor(random.nextDouble() * Math.floor(250));
        if (size % 2 != 0) {
            size++;
        }
        var chars = new String[size];
        for (var i = 0; i < size; i++) {
            var number = (int) Math.floor(random.nextDouble() * 16);
            chars[i] = Integer.toString(number, 16);
        }

        var randomString = String.join("", chars);
        var bytes = randomString.getBytes(StandardCharsets.UTF_8);
        return HexStringUtils.bytesToHexString(bytes);
    }


}
