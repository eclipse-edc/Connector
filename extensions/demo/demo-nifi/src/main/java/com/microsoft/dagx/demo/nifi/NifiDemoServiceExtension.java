package com.microsoft.dagx.demo.nifi;

import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataEntryExtensions;

/**
 * Loads data for the Nifi-based demo.
 */
public class NifiDemoServiceExtension implements ServiceExtension {
    private Monitor monitor;
    private ServiceExtensionContext context;

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        this.context = context;

        monitor.info("Initialized Nifi Demo extension");
    }

    @Override
    public void start() {
        saveDataEntries();

        monitor.info("Started Nifi Demo extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Nifi Demo extension");
    }

    private void saveDataEntries() {
        GenericDataEntryExtensions extensions = GenericDataEntryExtensions.Builder.newInstance().property("processGroup", "ee3eb39c-3c08-422a-a5e0-797d33031070").build();
        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().id("dagx:microsoft:artifacts:test").extensions(extensions).build();
        context.getService(MetadataStore.class).save(entry);
    }

}
