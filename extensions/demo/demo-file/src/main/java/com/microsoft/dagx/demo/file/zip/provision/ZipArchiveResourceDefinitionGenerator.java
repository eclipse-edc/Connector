/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.demo.file.zip.provision;

import com.microsoft.dagx.demo.file.zip.schema.ZipSchema;
import com.microsoft.dagx.spi.transfer.provision.ResourceDefinitionGenerator;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ZipArchiveResourceDefinitionGenerator implements ResourceDefinitionGenerator {
    @Override
    public @Nullable
    ResourceDefinition generate(TransferProcess process) {

        if (process.getDataRequest().getDestinationType() == null ||
                !process.getDataRequest().getDestinationType().equals("edc:zipArchive")) {
            return null;
        }

        var directory = process.getDataRequest().getDataDestination().getProperty(ZipSchema.DIRECTORY);
        var name = process.getDataRequest().getDataDestination().getProperty(ZipSchema.NAME);

        return ZipArchiveResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .zipFileName(name)
                .directory(directory).build();
    }
}
