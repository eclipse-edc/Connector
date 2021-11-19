package org.eclipse.dataspaceconnector.dataloader.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
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

    @BeforeEach
    void setUp() {
        assetLoaderMock = strictMock(AssetLoader.class);
        loadCommand = new LoadCommand(new ObjectMapper(), assetLoaderMock);

    }

    @Test
    void runCommand() {
        assetLoaderMock.accept(anyObject(AssetEntry.class));
        expectLastCall().times(10);
        replay(assetLoaderMock);

        var file = getFileFromResourceName("assets.json");
        loadCommand.setFile(file);
        loadCommand.setParseAssets(true);

        loadCommand.run();
        verify(assetLoaderMock);
    }

    @Test
    void runCommand_noFlagSet() {
        replay(assetLoaderMock);

        var file = getFileFromResourceName("assets.json");
        loadCommand.setFile(file);
        loadCommand.setParseAssets(null);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(IllegalArgumentException.class);
        verify(assetLoaderMock);
    }

    @Test
    void runCommand_flagNotTrue() {
        replay(assetLoaderMock);

        var file = getFileFromResourceName("assets.json");
        loadCommand.setFile(file);
        loadCommand.setParseAssets(false);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(IllegalArgumentException.class);
        verify(assetLoaderMock);
    }


    @Test
    void runCommand_fileNotValidContent() {
        replay(assetLoaderMock);

        var file = getFileFromResourceName("invalidContent.json");
        loadCommand.setFile(file);
        loadCommand.setParseAssets(true);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(RuntimeException.class).hasRootCauseInstanceOf(MismatchedInputException.class);
        verify(assetLoaderMock);
    }


    @Test
    void runCommand_fileNotExist() {
        replay(assetLoaderMock);

        var file = new File("/not/exist/foo.json");
        loadCommand.setFile(file);
        loadCommand.setParseAssets(true);

        assertThatThrownBy(() -> loadCommand.run()).isInstanceOf(RuntimeException.class).hasRootCauseInstanceOf(NoSuchFileException.class);
        verify(assetLoaderMock);
    }


}