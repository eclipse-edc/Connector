package org.eclipse.dataspaceconnector.samples.identity.registrationservice.crawler;

import org.eclipse.dataspaceconnector.ion.spi.DidStore;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.samples.identity.registrationservice.events.CrawlerEventPublisher;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

/**
 * Stores parameters (such as the DID Type) and necessary objects (such as the IonClient or the DidStore)
 * for the crawler, so it is essentially a Holder which is passed through to the {@link  CrawlerJob} by Quartz
 */
public class CrawlerContext {
    public static final String KEY = "edc:ion-crawler:config";
    private DidStore didStore;
    private Monitor monitor;
    private CrawlerEventPublisher publisher;
    private String ionHost;
    private String didTypes;
    private IonClient ionClient;
    private boolean randomize = false;

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

    public boolean shouldRandomize() {
        return randomize;
    }

    public IonClient getIonClient() {
        return ionClient;
    }

    public static final class Builder {
        private DidStore didStore;
        private Monitor monitor;
        private CrawlerEventPublisher publisher;
        private String ionHost;
        private String didTypes;
        private boolean randomize;
        private IonClient ionClient;

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

        public Builder randomize(boolean randomize) {
            this.randomize = randomize;
            return this;
        }

        public Builder didTypes(String didTypes) {
            this.didTypes = didTypes;
            return this;
        }

        public Builder ionClient(IonClient client) {
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
            crawlerConfig.randomize = randomize;
            crawlerConfig.ionClient = ionClient;
            return crawlerConfig;
        }
    }
}
