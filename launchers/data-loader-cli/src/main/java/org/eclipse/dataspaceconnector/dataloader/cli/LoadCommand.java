/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataloader.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.DataLoader;
import org.eclipse.dataspaceconnector.dataloading.DataSink;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

@CommandLine.Command(name = "load", mixinStandardHelpOptions = true, description = "Reads objects from a JSON file into a backing store.")
public class LoadCommand implements Runnable {

    private final ObjectMapper mapper;
    private final DataSink<AssetEntry> assetSink;
    private final DataSink<ContractDefinition> contractsSink;
    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    LoadInstruction loadInstruction;

    public LoadCommand(ObjectMapper mapper, DataSink<AssetEntry> assetSink, DataSink<ContractDefinition> contractsSink) {
        this.mapper = mapper;
        this.assetSink = assetSink;
        this.contractsSink = contractsSink;
    }

    @Override
    public void run() {
        if (loadInstruction == null) {
            throw new IllegalArgumentException("Please specify either the --assets flag or the --contracts flag");
        }
        if (loadInstruction.assetsFileName != null && loadInstruction.contractsFile != null) {
            throw new IllegalArgumentException("--assets and --contracts are mutually exclusive!");
        }
        if (loadInstruction.assetsFileName != null) {
            var json = readFile(loadInstruction.assetsFileName);
            var assetList = tryReadAsAssetRecords(json);

            DataLoader.Builder<AssetEntry> builder = DataLoader.Builder.newInstance();
            builder.sink(assetSink).build().insertAll(assetList);
        } else if (loadInstruction.contractsFile != null) {
            var json = readFile(loadInstruction.contractsFile);
            var contractDefList = tryReadAsContractRecords(json);
            DataLoader.Builder<ContractDefinition> builder = DataLoader.Builder.newInstance();
            builder.sink(contractsSink).build().insertAll(contractDefList);
        } else {
            throw new IllegalArgumentException("Can only parse AssetEntry objects at this time!");
        }
    }

    //needed for testing
    void setParseAssets(File parseAssets) {
        if (loadInstruction == null) {
            loadInstruction = new LoadInstruction();
        }
        loadInstruction.assetsFileName = parseAssets;
    }

    //needed for testing
    void setParseContracts(File parseAssets) {
        if (loadInstruction == null) {
            loadInstruction = new LoadInstruction();
        }
        loadInstruction.contractsFile = parseAssets;
    }

    private Collection<ContractDefinition> tryReadAsContractRecords(String json) {
        var tr = new TypeReference<Collection<ContractDefinition>>() {
        };
        try {
            return mapper.readValue(json, tr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<AssetEntry> tryReadAsAssetRecords(String json) {
        var tr = new TypeReference<Collection<AssetEntry>>() {
        };
        try {
            return mapper.readValue(json, tr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String readFile(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class LoadInstruction {
        @CommandLine.Option(names = { "-a", "--assets" }, required = true, description = "If specified, the contents of the JSON file will be interpreted as assets with data addresses")
        File assetsFileName;
        @CommandLine.Option(names = { "-b", "--contracts" }, required = true, description = "The JSON file that contains a list of objects (Assets, Contracts,...).")
        File contractsFile;
    }
}
