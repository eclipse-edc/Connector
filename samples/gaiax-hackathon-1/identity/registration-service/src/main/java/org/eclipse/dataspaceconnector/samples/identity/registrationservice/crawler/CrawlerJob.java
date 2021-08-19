package org.eclipse.dataspaceconnector.samples.identity.registrationservice.crawler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.iam.ion.IonClient;
import org.eclipse.dataspaceconnector.iam.ion.IonRequestException;
import org.eclipse.dataspaceconnector.iam.ion.crypto.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.Service;
import org.jetbrains.annotations.Nullable;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

// has to be "public", otherwise quartz won't be able to access is
public class CrawlerJob implements Job {

    private static final String IDENTIFIERS_PATH = "identifiers";
    private static final String DIDS_PATH = "dids";
    private String ionApiUrl = "http://23.97.144.59:3000/";

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        var cc = (CrawlerContext) jobDataMap.get(CrawlerContext.KEY);

        ionApiUrl = Objects.requireNonNull(cc.getIonHost(), "ION Node URL cannot be null!");

        // get latest did document to obtain continuation token
        var latestDocument = cc.getDidStore().getLatest();


        var continuationToken = latestDocument != null ? latestDocument.getId() : null;

        var monitor = cc.getMonitor();
        monitor.info("CrawlerJob: browsing ION to obtain new DIDs" + (continuationToken != null ? ", starting at " + continuationToken : ""));

        List<DidDocument> newDids;
        var start = Instant.now();
        if (cc.shouldRandomize()) {
            newDids = getRandomizedDid();
        } else {
            var newDidFutures = getDidDocumentsFromBlockchainAsync(continuationToken, cc);

            newDids = newDidFutures.parallelStream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        monitor.info("CrawlerJob: found " + newDids.size() + " new dids on ION, took " + (Duration.between(start, Instant.now()).toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase()));

        if (newDids.size() > 0) {
            cc.getPublisher().discoveryFinished(newDids.size());
        }

        cc.getDidStore().saveAll(newDids);


    }

    private List<CompletableFuture<DidDocument>> getDidDocumentsFromBlockchainAsync(String continuationToken, CrawlerContext context) {
        return getDidSuffixesForType(continuationToken, context.getDidTypes())
                .stream()
                .map(didSuffix -> resolveDidAsync(didSuffix, context.getIonClient()))
                .collect(Collectors.toList());
    }

    private List<String> getDidSuffixesForType(@Nullable String continuationToken, String type) {
        var client = createClient();

        var url = HttpUrl.parse(ionApiUrl)
                .newBuilder()
                .addPathSegment(DIDS_PATH)
                .addQueryParameter("type", type)
                .addQueryParameter("limit", "50"); //go a maximum of 50 transactions back


        var request = new Request.Builder()
                .url(url.build().url())
                .get()
                .build();

        try (var response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                var json = Objects.requireNonNull(response.body()).string();
                var om = new ObjectMapper();
                var tr = new TypeReference<List<String>>() {
                };
                return om.readValue(json, tr);
            } else {
                throw new IonRequestException(format("Could not get DIDs: error=%s, message=%s", response.code(), response.body().string()));
            }
        } catch (IOException e) {
            throw new IonRequestException(e);
        }
    }

    private DidDocument resolveDid(String didSuffix, IonClient ionClient) {
        try {
            return ionClient.resolve(didSuffix);
        } catch (IonRequestException ex) {
            return null;
        }
    }

    private CompletableFuture<DidDocument> resolveDidAsync(String didSuffix, IonClient ionClient) {
        return CompletableFuture.supplyAsync(() -> resolveDid(didSuffix, ionClient));
    }

    private OkHttpClient createClient() {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    }

    private List<DidDocument> getRandomizedDid() {


        // TODO: once the query API for Ion is in place, we need to actually query it
        // using the continuation token instead of generating random dids here :)
        var random = new SecureRandom();

        int howMany = random.nextInt(5);
        var results = new ArrayList<DidDocument>();

        for (int i = 0; i < howMany; i++) {

            byte[] r = new byte[32];
            random.nextBytes(r);
            String s = Base64.getUrlEncoder().encodeToString(r);

            var randomDocument = DidDocument.Builder.create()
                    .id("did:ion:" + s)
                    .authentication(Collections.singletonList("#key-1"))
                    .service(Collections.singletonList(new Service("#domain-1", "LinkedDomains", "https://test.service.com")))
                    .verificationMethod("#key-1", "EcdsaSecp256k1VerificationKey2019", (ECKey) KeyPairFactory.generateKeyPair().getPublicKey())
                    .build();

            results.add(randomDocument);
        }
        return results;
    }

}
