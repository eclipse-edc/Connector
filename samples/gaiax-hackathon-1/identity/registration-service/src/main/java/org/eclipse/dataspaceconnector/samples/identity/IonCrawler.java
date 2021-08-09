package org.eclipse.dataspaceconnector.samples.identity;

import org.eclipse.dataspaceconnector.iam.ion.IonClient;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.spi.iam.ObjectStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

class IonCrawler implements Crawler {
    private final Monitor monitor;
    private final ObjectStore<DidDocument> didDocumentStore;
    private final IonClient ionClient;

    public IonCrawler(Monitor monitor, ObjectStore<DidDocument> didDocumentStore, IonClient ionClient) {
        this.monitor = monitor;
        this.didDocumentStore = didDocumentStore;
        this.ionClient = ionClient;
    }

    @Override
    public void run() {
        //get "highest" did ID
        var latestDocument = didDocumentStore.getLatest();

        didDocumentStore.save(new DidDocument());
        monitor.info("Crawler ran through all ION entries");
    }
}
