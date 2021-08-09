package org.eclipse.dataspaceconnector.samples.identity;

import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.spi.iam.ObjectStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

class IonCrawler implements Crawler {
    private final Monitor monitor;
    private final ObjectStore<DidDocument> didDocumentStore;

    public IonCrawler(Monitor monitor, ObjectStore<DidDocument> didDocumentStore) {
        this.monitor = monitor;
        this.didDocumentStore = didDocumentStore;
    }

    @Override
    public void run() {
        didDocumentStore.save(new DidDocument());
        monitor.info("Crawler ran through all ION entries");
    }
}
