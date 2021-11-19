package org.eclipse.dataspaceconnector.dataloader.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.dataloading.DataLoader;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

@CommandLine.Command(name = "load", mixinStandardHelpOptions = true,
        description = "Reads objects from a JSON file into a backing store.")
public class LoadCommand implements Runnable {

    private final ObjectMapper mapper;
    private final AssetLoader sink;


    @CommandLine.Option(names = { "--assets" }, description = "If specified, the contents of the JSON file will be interpreted as assets with data addresses", negatable = false)
    private Boolean parseAssets;

    @CommandLine.Parameters(index = "0", description = "The JSON file that contains a list of objects (Assets, Contracts,...).")
    private File file;

    public LoadCommand(ObjectMapper mapper, AssetLoader sink) {
        this.mapper = mapper;
        this.sink = sink;
    }

    @Override
    public void run() {
        if (parseAssets == null) {
            throw new IllegalArgumentException("The --assets flag must be specified");
        }
        if (parseAssets) {
            var json = readFile(file);
            var assetList = tryReadAsAssetRecords(json);

            DataLoader.Builder<AssetEntry> builder = DataLoader.Builder.newInstance();
            builder.sink(sink).build().insertAll(assetList);
        } else {
            throw new IllegalArgumentException("Can only parse AssetEntry objects at this time!");
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
}
