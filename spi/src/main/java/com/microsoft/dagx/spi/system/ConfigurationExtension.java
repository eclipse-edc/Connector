package com.microsoft.dagx.spi.system;

import org.jetbrains.annotations.Nullable;

/**
 * Contributes configuration to a runtime. Multlple configuration extensions may be loaded in a runtime.
 */
public interface ConfigurationExtension extends BootExtension {

    /**
     * Returns the configuration setting for the key or null if not found.
     */
    @Nullable
    String getSetting(String key);

}
