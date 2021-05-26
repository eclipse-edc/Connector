package com.microsoft.dagx.demo.file;

import com.microsoft.dagx.demo.file.transfer.FileFlowController;
import com.microsoft.dagx.demo.file.transfer.provision.FolderResourceDefinitionGenerator;
import com.microsoft.dagx.demo.file.transfer.provision.RandomFileArtifactResourceDefinitionGenerator;
import com.microsoft.dagx.demo.file.transfer.provision.artifact.RandomFileArtifactProvisioner;
import com.microsoft.dagx.demo.file.transfer.provision.folder.FolderProvisioner;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.policy.PolicyRegistry;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;

import java.util.Set;

public class DemoFileExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var processManager = context.getService(TransferProcessManager.class);
        var processStore = context.getService(TransferProcessStore.class);
        var dataFlowManager = context.getService(DataFlowManager.class);
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        var provisionManager = context.getService(ProvisionManager.class);

        // Register API
        var webService = context.getService(WebService.class);
        webService.registerController(new DemoFileApiController(processManager, monitor));

        // Register Provisioner
        var folderDefGenerator = new FolderResourceDefinitionGenerator();
        var fileArtifactDefGenerator = new RandomFileArtifactResourceDefinitionGenerator();
        manifestGenerator.registerClientGenerator(folderDefGenerator);
        manifestGenerator.registerProviderGenerator(fileArtifactDefGenerator);

        provisionManager.register(new RandomFileArtifactProvisioner(monitor));
        provisionManager.register(new FolderProvisioner(monitor));

        // Register Data Flow
        var flowController = new FileFlowController(processStore, monitor);
        dataFlowManager.register(flowController);

        // Generate Demo Data
        generateDemoData(context);

        monitor.info("Initialized Demo File extension");
    }

    @Override
    public void start() {
        monitor.info("Started Demo File extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Demo File extension");
    }

    @Override
    public Set<String> requires() {
        return Set.of("policy-registry");
    }

    private void generateDemoData(ServiceExtensionContext context) {
        var metadataStore = context.getService(MetadataStore.class);
        var policyRegistry = context.getService(PolicyRegistry.class);
        var dataGenerator = new DemoMetaDataGenerator(metadataStore, policyRegistry);
        dataGenerator.generate("file", 10); // generates metadata "file1", "file2", "file3", ..
    }
}
