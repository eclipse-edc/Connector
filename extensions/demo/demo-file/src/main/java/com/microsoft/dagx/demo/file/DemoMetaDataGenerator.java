package com.microsoft.dagx.demo.file;

import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.policy.PolicyRegistry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataCatalog;

import java.util.UUID;

public class DemoMetaDataGenerator {
    private static final String EmptyPolicyId = "EmptyPolicy";
    private final MetadataStore metadataStore;

    public DemoMetaDataGenerator(MetadataStore metadataStore, PolicyRegistry policyRegistry) {
        this.metadataStore = metadataStore;
        var emptyPolicy = Policy.Builder.newInstance().id(EmptyPolicyId).build();
        policyRegistry.registerPolicy(emptyPolicy);
    }

    public void generate(String prefix, int count) {
        var catalog = GenericDataCatalog.Builder.newInstance()
                .property("demoGroup", UUID.randomUUID().toString())
                .build();

        for (var i = 1; i <= count; i++) {
            var entry = DataEntry.Builder.newInstance().id(prefix + i).policyId(EmptyPolicyId).catalog(catalog).build();
            metadataStore.save(entry);
        }
    }
}
