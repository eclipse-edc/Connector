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

package org.eclipse.dataspaceconnector.examples;

import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Disabled
public class JsonFileGenerator {

    @Test
    void createAssetEntries() throws IOException {
        var entries = IntStream.range(0, 10).mapToObj(i -> new AssetEntry(createAsset(i), createDataAddress(i)))
                .collect(Collectors.toUnmodifiableList());
        var json = new TypeManager().getMapper().writeValueAsString(entries);
        Files.write(Path.of("/home/paul/Documents/assets.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void createContractDefinitions() throws IOException {
        var entries = IntStream.range(0, 10).mapToObj(this::createContractDefinition)
                .collect(Collectors.toUnmodifiableList());
        var json = new TypeManager().getMapper().writeValueAsString(entries);
        Files.write(Path.of("/home/paul/Documents/contracts.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    private ContractDefinition createContractDefinition(int i) {

        return ContractDefinition.Builder.newInstance()
                .id("ContractDefinition_" + i)
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .contractPolicyId("CP_" + i)
                .accessPolicyId("AP_" + i)
                .build();
    }

    private DataAddress createDataAddress(int i) {
        return DataAddress.Builder.newInstance()
                .type("test-dataaddress-" + i)
                .property("someprop", "someval")
                .build();
    }

    private Asset createAsset(int i) {
        return Asset.Builder.newInstance()
                .id("test-asset-" + i)
                .name("test-asset-" + i)
                .version("1.0")
                .build();
    }
}
