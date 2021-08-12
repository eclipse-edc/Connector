package org.eclipse.dataspaceconnector.samples.identity.registrationservice.crawler;

import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.ion.crypto.KeyPairFactory;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.Service;
import org.eclipse.dataspaceconnector.iam.ion.spi.DidStore;
import org.eclipse.dataspaceconnector.samples.identity.registrationservice.events.CrawlerEventPublisher;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

// has to be "public", otherwise quartz won't be able to access is
public class CrawlerJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        var store = (DidStore) context.getJobDetail().getJobDataMap().get("STORE");
        var monitor = (Monitor) context.getJobDetail().getJobDataMap().get("MONITOR");
        var publisher = (CrawlerEventPublisher) context.getJobDetail().getJobDataMap().get("PUBLISHER");


        // get latest did document to obtain continuation token
        var latestDocument = store.getLatest();


        var continuationToken = latestDocument != null ? latestDocument.getId() : null;

        monitor.info("CrawlerJob: browsing ION to obtain new DIDs" + (continuationToken != null ? ", starting at " + continuationToken : ""));
        var newDids = getNewDidsFromIon(continuationToken);
        monitor.info("CrawlerJob: found " + newDids.size() + " new dids on ION");

        if (newDids.size() > 0) {
            publisher.discoveryFinished(newDids.size());
        }

        store.saveAll(newDids);

    }

    private List<DidDocument> getNewDidsFromIon(@Nullable String continuationToken) {


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
