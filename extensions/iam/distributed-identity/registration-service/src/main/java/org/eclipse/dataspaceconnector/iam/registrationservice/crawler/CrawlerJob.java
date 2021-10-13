/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.registrationservice.crawler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
                .filter(result -> !result.invalid())
                .map(DidResolverRegistry.Result::getDidDocument)
                .collect(Collectors.toList());

        monitor.info("CrawlerJob: Found " + newDids.size() + " new DIDs on ION, took " + (Duration.between(start, Instant.now()).toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase()));

        if (newDids.size() > 0) {
            cc.getPublisher().discoveryFinished(newDids.size());
        }

        cc.getDidStore().saveAll(newDids);


    }

    private List<CompletableFuture<DidResolverRegistry.Result>> getDidDocumentsFromBlockchainAsync(CrawlerContext context) {
        return getDidSuffixesForType(context.getDidTypes())
                .stream()
                .map(didSuffix -> resolveDidAsync(didSuffix, context.getResolverRegistry()))
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
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

        var uri = ionApiUrl + "/" + DIDS_PATH + "?limit=50&type=" + type;

        try {
            var request = HttpRequest.newBuilder(new URI(uri))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                var json = Objects.requireNonNull(response.body());
                var om = new ObjectMapper();
                var tr = new TypeReference<List<String>>() {};
                return om.readValue(json, tr);
            } else {
                throw new EdcException(format("Could not get DIDs: error=%s, message=%s", response.statusCode(), response.body()));
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new EdcException(e);
        }
    }

    /**
     * Attempts to resolve a DID from ION
     *
     * @param didId The canonical ID (="suffix", "short form URI") of the DID. Must be in the form "did:ion:..."
     * @param resolverRegistry The resolver registry
     * @return A {@link DidDocument} if found, {@code null} otherwise
     */
    private DidResolverRegistry.Result resolveDid(String didId, DidResolverRegistry resolverRegistry) {
        try {
            return resolverRegistry.resolve(didId);
        } catch (EdcException ex) {
            return null;
        }
    }

    /**
     * Attempts to resolve a DID from ION asynchronously. Basically a wrapper around {@link CrawlerJob#resolveDid(String, DidResolverRegistry)}
     *
     * @param didId The canonical ID (="suffix", "short form URI") of the DID. Must be in the form "did:ion:..."
     * @param resolverRegistry An ION implementation
     * @return A {@code CompletableFuture<DidDocument>} if found, {@code null} otherwise
     */
    private CompletableFuture<DidResolverRegistry.Result> resolveDidAsync(String didId, DidResolverRegistry resolverRegistry) {
        return CompletableFuture.supplyAsync(() -> resolveDid(didId, resolverRegistry));
    }

}
