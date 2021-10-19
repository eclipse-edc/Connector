package org.eclipse.dataspaceconnector.iam.registrationservice.crawler;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.eclipse.dataspaceconnector.iam.registrationservice.events.CrawlerEventPublisher;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

/**
 * Stores parameters (such as the DID Type) and necessary objects (such as the IonClient or the DidStore)
 * for the crawler, so it is essentially a Holder which is passed through to the {@link CrawlerJob} by Quartz
 */
public class CrawlerContext {
    public static final String KEY = "edc:ion-crawler:config";
    private DidStore didStore;
    private Monitor monitor;
    private CrawlerEventPublisher publisher;
    private String ionHost;
    private String didTypes;
    private DidResolver ionClient;

    public DidStore getDidStore() {
        return didStore;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public CrawlerEventPublisher getPublisher() {
        return publisher;
    }

    public String getIonHost() {
        return ionHost;
    }

    public String getDidTypes() {
        return didTypes;
    }


    public DidResolver getIonClient() {
        return ionClient;
    }

    public static final class Builder {
        private DidStore didStore;
        private Monitor monitor;
        private CrawlerEventPublisher publisher;
        private String ionHost;
        private String didTypes;
        private DidResolver ionClient;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder didStore(DidStore didStore) {
            this.didStore = didStore;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder publisher(CrawlerEventPublisher publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder ionHost(String ionHost) {
            this.ionHost = ionHost;
            return this;
        }

        public Builder didTypes(String didTypes) {
            this.didTypes = didTypes;
            return this;
        }

        public Builder ionClient(DidResolver client) {
            ionClient = client;
            return this;
        }

        public CrawlerContext build() {
            CrawlerContext crawlerConfig = new CrawlerContext();
            crawlerConfig.didTypes = didTypes;
            crawlerConfig.ionHost = ionHost;
            crawlerConfig.publisher = publisher;
            crawlerConfig.didStore = (DidStore) didStore;
            crawlerConfig.monitor = monitor;
            crawlerConfig.ionClient = ionClient;
            return crawlerConfig;
        }
    }
}
