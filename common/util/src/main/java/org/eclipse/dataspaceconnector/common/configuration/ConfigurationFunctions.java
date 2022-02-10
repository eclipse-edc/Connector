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

package org.eclipse.dataspaceconnector.common.configuration;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Common configuration functions used by extensions.
 */
public class ConfigurationFunctions {

    /**
     * Returns the property value, env value or default value for the key.
     * <p>Naming conventions for keys are '[qualifier].[value]' in lower case. When checking for env variables, keys will be converted to uppercase and '.' replaced by '_'.</p>
     */
    public static String propOrEnv(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        String upperKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(upperKey);
        return value != null ? value : defaultValue;
    }

    /**
     * Utility method to find an unallocated port. Note that there is a race condition,
     * the port might be allocated by the time it is used.
     *
     * @return a server port.
     */
    public static int findUnallocatedServerPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
