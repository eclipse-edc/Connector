package org.eclipse.dataspaceconnector.extensions.listener;

import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;

public class TransferListenerExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var transferProcessObservable = context.getService(TransferProcessObservable.class);
        var monitor = context.getMonitor();

        transferProcessObservable.registerListener(new MarkerFileCreator(monitor));
    }
}
