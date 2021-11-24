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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.dataloading.DataSink;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.NoSuchFileException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFileFromResourceName;

class LoadCommandTest {
    private LoadCommand loadCommand;
    private AssetLoader assetLoaderMock;
    private DataSink<ContractDefinition> contractsSinkMock;

    @BeforeEach
    void setUp() {
        assetLoaderMock = strictMock(AssetLoader.class);
        contractsSinkMock = strictMock(ContractDefinitionLoader.class);
        loadCommand = new LoadCommand(new ObjectMapper(), assetLoaderMock, contractsSinkMock);

    }

    @Test
    void runCommand_assets() {
        assetLoaderMock.accept(anyObject(AssetEntry.class));
        expectLastCall().times(10);
        replay(assetLoaderMock);

        var file = getFileFromResourceName("assets.json");
        loadCommand.setParseAssets(file);

        loadCommand.run();
        verify(assetLoaderMock);
    }

    @Test
    void runCommand_assets_noFlagSet() {
        replay(assetLoaderMock);

        var file = getFileFromResourceName("assets.json");
        loadCommand.setParseAssets(null);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(IllegalArgumentException.class);
        verify(assetLoaderMock);
    }

    @Test
    void runCommand_assets_fileNotValidContent() {
        replay(assetLoaderMock);

        var file = getFileFromResourceName("invalidContent.json");
        loadCommand.setParseAssets(file);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(RuntimeException.class).hasRootCauseInstanceOf(MismatchedInputException.class);
        verify(assetLoaderMock);
    }

    @Test
    void runCommand_assets_fileNotExist() {
        replay(assetLoaderMock);

        var file = new File("/not/exist/foo.json");
        loadCommand.setParseAssets(file);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(RuntimeException.class).hasRootCauseInstanceOf(NoSuchFileException.class);
        verify(assetLoaderMock);
    }

    @Test
    void runCommand_contracts() {
        contractsSinkMock.accept(anyObject(ContractDefinition.class));
        expectLastCall().times(10);
        replay(contractsSinkMock, assetLoaderMock);

        var file = getFileFromResourceName("contracts.json");
        loadCommand.setParseContracts(file);

        loadCommand.run();
        verify(contractsSinkMock, assetLoaderMock);
    }

    @Test
    void runCommand_contracts_noFlagSet() {
        replay(assetLoaderMock, contractsSinkMock);

        loadCommand.setParseContracts(null);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(IllegalArgumentException.class);
        verify(assetLoaderMock, contractsSinkMock);
    }

    @Test
    void runCommand_contracts_fileNotValidContent() {
        replay(assetLoaderMock, contractsSinkMock);

        var file = getFileFromResourceName("invalidContent.json");
        loadCommand.setParseContracts(file);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(RuntimeException.class).hasRootCauseInstanceOf(MismatchedInputException.class);
        verify(assetLoaderMock, contractsSinkMock);
    }

    @Test
    void runCommand_contracts_fileNotExist() {
        replay(assetLoaderMock, contractsSinkMock);

        var file = new File("/not/exist/foo.json");
        loadCommand.setParseContracts(file);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(RuntimeException.class).hasRootCauseInstanceOf(NoSuchFileException.class);
        verify(assetLoaderMock, contractsSinkMock);
    }

    @Test
    void runCommand_bothFlagsSet_throwsException() {
        replay(assetLoaderMock, contractsSinkMock);

        var file1 = getFileFromResourceName("assets.json");
        var file2 = getFileFromResourceName("contracts.json");

        loadCommand.setParseAssets(file1);
        loadCommand.setParseContracts(file2);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(IllegalArgumentException.class);
    }


}