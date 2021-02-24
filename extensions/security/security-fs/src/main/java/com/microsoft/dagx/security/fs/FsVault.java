package com.microsoft.dagx.security.fs;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implements a vault backed by a properties file.
 */
public class FsVault implements Vault {
    private Map<String, String> secrets = new HashMap<>();

    public FsVault(Path vaultFile) {
        try (final InputStream is = Files.newInputStream(vaultFile)) {
            var properties = new Properties();
            properties.load(is);
            for (String name : properties.stringPropertyNames()) {
                secrets.put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            throw new DagxException(e);
        }
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        return secrets.get(key);
    }
}
