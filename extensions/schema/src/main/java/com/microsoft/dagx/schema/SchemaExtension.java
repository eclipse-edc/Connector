package com.microsoft.dagx.schema;

import com.microsoft.dagx.schema.aws.S3BucketSchema;
import com.microsoft.dagx.schema.azure.AzureSchema;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

import java.util.Set;

public class SchemaExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(SchemaRegistry.FEATURE);
    }

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor= context.getMonitor();

        var sr= new SchemaRegistryImpl();
        sr.register(new AzureSchema());
        sr.register(new S3BucketSchema());

        context.registerService(SchemaRegistry.class, sr);
        monitor.info("Initialized Schema Registry");

    }
}

