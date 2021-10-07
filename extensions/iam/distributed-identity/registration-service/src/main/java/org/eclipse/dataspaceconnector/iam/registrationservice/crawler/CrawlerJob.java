package org.eclipse.dataspaceconnector.iam.registrationservice.crawler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

// has to be "public", otherwise quartz won't be able to access is
public class CrawlerJob implements Job {

    private static final String DIDS_PATH = "dids";
    private String ionApiUrl;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        var cc = (CrawlerContext) jobDataMap.get(CrawlerContext.KEY);

        ionApiUrl = Objects.requireNonNull(cc.getIonHost(), "ION Node URL cannot be null!");
        var monitor = cc.getMonitor();

        monitor.info("CrawlerJob: browsing ION to obtain GaiaX DIDs");

        List<DidDocument> newDids;
        var start = Instant.now();
        var newDidFutures = getDidDocumentsFromBlockchainAsync(cc);

        newDids = newDidFutures.parallelStream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        monitor.info("CrawlerJob: found " + newDids.size() + " new dids on ION, took " + (Duration.between(start, Instant.now()).toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase()));

        if (newDids.size() > 0) {
            cc.getPublisher().discoveryFinished(newDids.size());
        }

        cc.getDidStore().saveAll(newDids);


    }

    private List<CompletableFuture<DidDocument>> getDidDocumentsFromBlockchainAsync(CrawlerContext context) {
        return getDidSuffixesForType(context.getDidTypes())
                .stream()
                .map(didSuffix -> resolveDidAsync(didSuffix, context.getIonClient()))
                .collect(Collectors.toList());
    }

    /**
     * queries the ION Core API that maps Bitcoin transactions to IPFS CoreIndexFiles which have a "type" field equal to
     * the {@code type} parameter and returns the resulting DID suffixes (=IDs).
     *
     * @param type The type to look up. Should be "Z3hp" for GaiaX
     * @return A list of DID IDs in the form {@code did:ion:.....}
     */
    private List<String> getDidSuffixesForType(String type) {
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
                throw new EdcException(format("Could not get DIDs: error=%s, message=%s", response.code(), response.body().string()));
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    /**
     * Attempts to resolve a DID from ION
     *
     * @param didId     The canonical ID (="suffix", "short form URI") of the DID. Must be in the form "did:ion:..."
     * @param ionClient An ION implementation
     * @return A {@link DidDocument} if found, {@code null} otherwise
     */
    private DidDocument resolveDid(String didId, DidResolver ionClient) {
        try {
            return ionClient.resolve(didId);
        } catch (EdcException ex) {
            return null;
        }
    }

    /**
     * Attempts to resolve a DID from ION asynchronously. Basically a wrapper around {@link CrawlerJob#resolveDidAsync(String, DidResolver)}
     *
     * @param didId     The canonical ID (="suffix", "short form URI") of the DID. Must be in the form "did:ion:..."
     * @param ionClient An ION implementation
     * @return A {@code CompletableFuture<DidDocument>} if found, {@code null} otherwise
     */
    private CompletableFuture<DidDocument> resolveDidAsync(String didId, DidResolver ionClient) {
        return CompletableFuture.supplyAsync(() -> resolveDid(didId, ionClient));
    }

    private OkHttpClient createClient() {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    }

}
