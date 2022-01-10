package org.eclipse.dataspaceconnector.samples.identity.did;

import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(DidStore.class)
public class DidDocumentStoreExtension implements ServiceExtension {
    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new InMemoryDidDocumentStore();
        context.registerService(DidStore.class, store);
    }
}
