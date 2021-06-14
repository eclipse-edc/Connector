/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.demo.file;

import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.policy.PolicyRegistry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataCatalog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

public class DummyFileGenerator {
    private static final String EmptyPolicyId = "EmptyPolicy";
    private final MetadataStore metadataStore;

    public DummyFileGenerator(MetadataStore metadataStore, PolicyRegistry policyRegistry) {
        this.metadataStore = metadataStore;
        var emptyPolicy = Policy.Builder.newInstance().id(EmptyPolicyId).build();
        policyRegistry.registerPolicy(emptyPolicy);
    }

    public void generate(String prefix, int count) {
        try {

            var dir = Configuration.TmpDirectory + "/catalog";
            var dirPath = Paths.get(dir);
            if (Files.notExists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            for (var i = 1; i <= count; i++) {
                var name = prefix + i;
                var filePath = Files.writeString(Paths.get(dir, name), UUID.randomUUID().toString());

                var catalog = GenericDataCatalog.Builder.newInstance()
                        .property("type", "file")
                        .property("name", name)
                        .property("directory", dir)
                        .property("size", String.valueOf(Files.size(filePath)))
                        .property("content-type", "Text (text/plain)")
                        .property("modified", Files.getLastModifiedTime(filePath).toString())
                        .property("comment", "dummy data")
                        .build();

                var entry = DataEntry.Builder.newInstance().id(prefix + i).policyId(EmptyPolicyId).catalog(catalog)
                        .build();

                metadataStore.save(entry);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
