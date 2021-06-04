/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.configuration.fs;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ConfigurationExtension;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.microsoft.dagx.common.ConfigurationFunctions.propOrEnv;
import static java.lang.String.format;

/**
 * Sources configuration values from a properties file.
 */
public class FsConfigurationExtension implements ConfigurationExtension {
    private static final String CONFIG_LOCATION = propOrEnv("dagx.fs.config", "dagx-configuration.properties");

    private Path configFile;

    private final Map<String, String> propertyCache = new HashMap<>();

    /**
     * Default ctor - required for extension loading
     */
    @SuppressWarnings("unused")
    public FsConfigurationExtension() {
    }

    /**
     * Testing ctor
     */
    FsConfigurationExtension(Path configFile) {
        this.configFile = configFile;
    }

    @Override
    public void initialize(Monitor monitor) {
        var configPath = configFile != null ? configFile : Paths.get(CONFIG_LOCATION);
        if (!Files.exists(configPath)) {
            monitor.info(format("Configuration file does not exist: %s. Ignoring.", CONFIG_LOCATION));
            return;
        }

        try (final InputStream is = Files.newInputStream(configPath)) {
            var properties = new Properties();
            properties.load(is);
            for (String name : properties.stringPropertyNames()) {
                propertyCache.put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            throw new DagxException(e);
        }

        monitor.info("Initialized FS Configuration extension");
    }

    @Override
    public @Nullable String getSetting(String key) {
        return propertyCache.get(key);
    }
}
