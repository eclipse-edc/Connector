package org.eclipse.dataspaceconnector.ion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.eclipse.dataspaceconnector.ion.model.IonRequest;
import org.eclipse.dataspaceconnector.ion.model.did.resolution.DidDocument;
import org.eclipse.dataspaceconnector.ion.model.did.resolution.DidResolveResponse;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.ion.util.HexStringUtils;
import org.eclipse.dataspaceconnector.ion.util.JsonCanonicalizer;
import org.eclipse.dataspaceconnector.ion.util.SortingNodeFactory;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Standard implementation of the {@link IonClient} interface that can resolve DIDs from ION and submit anchor requests to id.
 */
public class DefaultIonClient implements IonClient {

    private final static String DEFAULT_RESOLUTION_ENDPOINT = "https://beta.discover.did.microsoft.com/1.0";
    private final static String IDENTIFIERS_PATH = "/identifiers";
    private final static String OPERATIONS_PATH = "/operations";
    private final String ionUrl;
    private final ObjectMapper typeManager;

    public DefaultIonClient(ObjectMapper typeManager) {
        this(DEFAULT_RESOLUTION_ENDPOINT, typeManager);
    }

    public DefaultIonClient(String ionEndpoint, ObjectMapper typeManager) {
        ionUrl = ionEndpoint;
        this.typeManager = typeManager;
    }

    @Override
    public DidDocument submit(IonRequest request) {
        String requestBodyJson;
        try {
            requestBodyJson = typeManager.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IonException(ex);
        }

        MediaType json = MediaType.get("application/json; charset=utf-8");
        RequestBody okHttpRequestbody = RequestBody.create(requestBodyJson, json);
        var solutionRequest = new Request.Builder()
                .url(ionUrl + OPERATIONS_PATH)
                .post(okHttpRequestbody)
                .header("Content-Type", "application/json")
                .build();

        var client = getOkHttpClient();

        try (var solutionResponse = client.newCall(solutionRequest).execute()) {
            String responseBodyJson = solutionResponse.body().string();
            if (solutionResponse.isSuccessful()) {
                var didResponse = typeManager.readValue(responseBodyJson, DidResolveResponse.class);
                return didResponse.getDidDocument();
            } else {
                if (solutionResponse.code() >= 500) {
                    throw new IonRequestException("Unexpected 5xx response " + responseBodyJson);
                } else if (solutionResponse.code() >= 400) {
                    //means bad request, should retry
                    throw new IonRequestException("Bad Request, please retry with another challenge");
                } else if (solutionResponse.code() >= 300) {
                    throw new IonRequestException("Unexpected 3xx response: " + solutionResponse.message());
                }
                throw new IonRequestException("Unexpected response: " + solutionResponse.code() + ", " + responseBodyJson);
            }

        } catch (Exception ex) {
            throw new IonException(ex);
        }
    }

    @Override
    public DidDocument resolve(String didUri) {
        var rq = new Request.Builder()
                .get()
                .url(ionUrl + IDENTIFIERS_PATH + "/" + didUri)
                .build();

        try (var response = getOkHttpClient().newCall(rq).execute()) {
            if (response.isSuccessful()) {
                var body = response.body().string();
                DidResolveResponse didResolveResponse = typeManager.readValue(body, DidResolveResponse.class);
                return didResolveResponse.getDidDocument();
            }
            throw new IonRequestException("resolving the DID URI was unsuccessful: code = " + response.code() + " content= " + response.body().string());
        } catch (Exception ex) {
            throw new IonRequestException(ex);
        }
    }

    public void submitWithChallengeResponse(IonRequest request, String challengeEndpoint, String solutionEndpoint) {
        System.out.println("Getting challenge from " + challengeEndpoint);

        OkHttpClient client = getOkHttpClient();
        ObjectMapper objectMapper = JsonMapper.builder()
                .nodeFactory(new SortingNodeFactory())
                .build();

        var rq = new Request.Builder().get()
                .url(challengeEndpoint)
                .build();
        String challengeNonce;
        String largestAllowedHash;
        int validDuration;

        try (var response = client.newCall(rq).execute()) {
            if (!response.isSuccessful()) {
                throw new IonException("Error obtaining challenge: " + response.message());
            }
            var json = Objects.requireNonNull(response.body()).string();
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
        String answerNonce, answerNonceHex;

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

        MediaType json = MediaType.get("application/json; charset=utf-8");
        RequestBody okHttpRequestbody = RequestBody.create(requestBodyJson, json);
        var solutionRequest = new Request.Builder()
                .url(solutionEndpoint)
                .post(okHttpRequestbody)
                .header("Challenge-Nonce", challengeNonce)
                .header("Answer-Nonce", answerNonce)
                .header("Content-Type", "application/json")
                .build();

        try (var solutionResponse = client.newCall(solutionRequest).execute()) {

            if (solutionResponse.isSuccessful()) {
                System.out.println("Successfully submitted the anchor request");
                var body = solutionResponse.body().string();
                var message = solutionResponse.message();

                System.out.println("Message: " + message);
                System.out.println("Body: " + body);
            } else {
                if (solutionResponse.code() >= 500) {
                    throw new IonRequestException("Unexpected 5xx response " + solutionResponse.body().string());
                } else if (solutionResponse.code() >= 400) {
                    //means bad request, should retry
                    throw new IonRequestException("Bad Request, please retry with another challenge");
                } else if (solutionResponse.code() >= 300) {
                    throw new IonRequestException("Unexpected 3xx response: " + solutionResponse.message());
                }
            }

        } catch (Exception ex) {
            throw new IonException(ex);
        }
    }

    @NotNull
    private OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder().build();
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
