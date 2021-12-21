/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.postgresql.contractdefinitionloader.settings;

import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryConfig;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConnectionFactoryConfigFactory {

    private static final String LOG_INVALID_NUMBER = "AssetLoader-PostgreSQL-Configuration: Expecting number in property %s";
    private static final String LOG_URL_USERNAME_REQUIRED = "AssetLoader-PostgreSQL-Configuration: Username (%s) and url (%s) must be provided in the settings";
    private static final String LOG_INVALID_URI = "AssetLoader-PostgreSQL-Configuration: Invalid URI syntax in setting %s.";
    private static final String LOG_FILE_NOT_EXISTS = "AssetLoader-PostgreSQL-Configuration: File does not exist: %s.";
    private static final String LOG_INVALID_PATH = "AssetLoader-PostgreSQL-Configuration: Invalid path in setting %s";
    private static final String LOG_INVALID_ENUM = "AssetLoader-PostgreSQL-Configuration: Invalid configuration for %s. Valid settings: %s";
    private static final String EXCEPTION_INVALID_SETTINGS = "AssetLoader-PostgreSQL-Configuration: Invalid or incomplete PostgreSQL settings.";

    private final ServiceExtensionContext serviceExtensionContext;
    private final Monitor monitor;

    public ConnectionFactoryConfigFactory(@NotNull ServiceExtensionContext serviceExtensionContext) {
        this.serviceExtensionContext = Objects.requireNonNull(serviceExtensionContext);
        this.monitor = serviceExtensionContext.getMonitor();
    }

    public ConnectionFactoryConfig create() {

        boolean invalidConfiguration = false;

        ConnectionFactoryConfig.Builder builder = ConnectionFactoryConfig.Builder.newInstance();

        // URL
        String url = getStringSetting(SettingKeys.POSTGRESQL_URL);
        if (url != null) {
            try {
                builder.uri(new URI(url));
            } catch (URISyntaxException ignore) {
                monitor.severe(String.format(LOG_INVALID_URI, SettingKeys.POSTGRESQL_URL));
                invalidConfiguration = true;
            }
        }

        // USERNAME
        String userName = getStringSetting(SettingKeys.POSTGRESQL_USERNAME);
        if (userName != null) {
            builder.userName(userName);
        }

        if (url == null || userName == null) {
            monitor.severe(String.format(LOG_URL_USERNAME_REQUIRED, SettingKeys.POSTGRESQL_URL, SettingKeys.POSTGRESQL_USERNAME));
            invalidConfiguration = true;
        }

        // PASSWORD
        String password = getStringSetting(SettingKeys.POSTGRESQL_PASSWORD);
        if (password != null) {
            builder.password(password);
        }

        // SSL
        Boolean ssl = getBooleanSetting(SettingKeys.POSTGRESQL_SSL);
        if (ssl != null) {
            builder.ssl(ssl);
        }

        // SSL MODE
        String sslMode = getStringSetting(SettingKeys.POSTGRESQL_SSL_MODE);
        if (sslMode != null) {
            try {
                builder.sslMode(
                        Arrays.stream(ConnectionFactoryConfig.SslMode.values())
                                .filter(l -> l.toString().equalsIgnoreCase(sslMode))
                                .findFirst()
                                .orElseThrow(IllegalArgumentException::new)
                );
            } catch (IllegalArgumentException ignore) {
                String enumValues = Arrays.stream(ConnectionFactoryConfig.SslMode.values())
                        .map(ConnectionFactoryConfig.SslMode::toString)
                        .collect(Collectors.joining(", "));
                monitor.severe(String.format(LOG_INVALID_ENUM, SettingKeys.POSTGRESQL_SSL_MODE, enumValues));
                invalidConfiguration = true;
            }
        }

        // SSL CERT
        String sslCert = getStringSetting(SettingKeys.POSTGRESQL_SSL_CERT);
        if (sslCert != null) {
            try {
                Path path = Path.of(sslCert);
                if (Files.exists(path)) {
                    builder.sslCert(path);
                } else {
                    monitor.severe(String.format(LOG_FILE_NOT_EXISTS, path));
                    invalidConfiguration = true;
                }
            } catch (InvalidPathException ignore) {
                monitor.severe(String.format(LOG_INVALID_PATH, SettingKeys.POSTGRESQL_SSL_CERT));
                invalidConfiguration = true;
            }
        }

        // SSL KEY
        String sslKey = getStringSetting(SettingKeys.POSTGRESQL_SSL_KEY);
        if (sslKey != null) {
            try {
                Path path = Path.of(sslKey);
                if (Files.exists(path)) {
                    builder.sslKey(Path.of(sslKey));
                } else {
                    monitor.severe(String.format(LOG_FILE_NOT_EXISTS, path));
                    invalidConfiguration = true;
                }
            } catch (InvalidPathException ignore) {
                monitor.severe(String.format(LOG_INVALID_PATH, SettingKeys.POSTGRESQL_SSL_KEY));
                invalidConfiguration = true;
            }
        }

        // SSL ROOT CERT
        String sslRootCert = getStringSetting(SettingKeys.POSTGRESQL_SSL_ROOT_CERT);
        if (sslRootCert != null) {
            builder.sslRootCert(Path.of(sslRootCert)); // don't check if file exists, because the sslRootCert may only be a file
        }

        // READONLY
        Boolean readonly = getBooleanSetting(SettingKeys.POSTGRESQL_SSL_READONLY);
        if (readonly != null) {
            builder.readOnly(readonly);
        }

        // SSL HOST NAME VERIFIER
        String sslHostNameVerifier = getStringSetting(SettingKeys.POSTGRESQL_SSL_HOSTNAME_VERIFIER);
        if (sslHostNameVerifier != null) {
            builder.sslHostNameVerifier(sslHostNameVerifier);
        }

        // LOGGER LEVEL
        String loggerLevel = getStringSetting(SettingKeys.POSTGRESQL_LOGGER_LEVEL);
        if (loggerLevel != null) {
            try {
                builder.loggerLevel(
                        Arrays.stream(ConnectionFactoryConfig.LoggerLevel.values())
                                .filter(l -> l.toString().equalsIgnoreCase(loggerLevel))
                                .findFirst()
                                .orElseThrow(IllegalArgumentException::new)
                );
            } catch (IllegalArgumentException ignore) {
                String enumValues = Arrays.stream(ConnectionFactoryConfig.LoggerLevel.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));
                monitor.severe(String.format(LOG_INVALID_ENUM, SettingKeys.POSTGRESQL_LOGGER_LEVEL, enumValues));
                invalidConfiguration = true;
            }
        }

        // LOGGER FILE
        String loggerFile = getStringSetting(SettingKeys.POSTGRESQL_LOGGER_FILE);
        if (loggerFile != null) {
            try {
                Path path = Path.of(loggerFile);
                builder.loggerFile(path);
            } catch (InvalidPathException ignore) {
                monitor.severe(String.format(LOG_INVALID_PATH, SettingKeys.POSTGRESQL_LOGGER_FILE));
            }
        }

        // LOG UNCLOSED CONNECTION
        Boolean logUnclosedConnection = getBooleanSetting(SettingKeys.POSTGRESQL_LOG_UNCLOSED_CONNECTION);
        if (logUnclosedConnection != null) {
            builder.logUnclosedConnections(logUnclosedConnection);
        }

        // SOCKET TIMEOUT
        try {
            Long socketTimeout = getLongSetting(SettingKeys.POSTGRESQL_SOCKET_TIMEOUT);
            if (socketTimeout != null) {
                builder.socketTimeout(socketTimeout);
            }
        } catch (NumberFormatException ignore) {
            monitor.severe(String.format(LOG_INVALID_NUMBER, SettingKeys.POSTGRESQL_SOCKET_TIMEOUT));
            invalidConfiguration = true;
        }

        // CONNECTION TIMEOUT
        try {
            Long connectionTimeout = getLongSetting(SettingKeys.POSTGRESQL_CONNECTION_TIMEOUT);
            if (connectionTimeout != null) {
                builder.connectTimeout(connectionTimeout);
            }
        } catch (NumberFormatException ignore) {
            monitor.severe(String.format(LOG_INVALID_NUMBER, SettingKeys.POSTGRESQL_CONNECTION_TIMEOUT));
            invalidConfiguration = true;
        }

        // LOGIN TIMEOUT
        try {
            Long loginTimeout = getLongSetting(SettingKeys.POSTGRESQL_LOGIN_TIMEOUT);
            if (loginTimeout != null) {
                builder.loginTimeout(loginTimeout);
            }
        } catch (NumberFormatException ignore) {
            monitor.severe(String.format(LOG_INVALID_NUMBER, SettingKeys.POSTGRESQL_SOCKET_TIMEOUT));
            invalidConfiguration = true;
        }

        // APPLICATION NAME
        String applicationName = getStringSetting(SettingKeys.POSTGRESQL_APPLICATION_NAME);
        if (applicationName != null) {
            builder.applicationName(applicationName);
        }

        if (invalidConfiguration) {
            throw new EdcException(EXCEPTION_INVALID_SETTINGS);
        } else {
            return builder.build();
        }
    }

    private Long getLongSetting(String key) {
        String setting = serviceExtensionContext.getSetting(key, null);
        return setting == null ? null : Long.parseLong(setting);
    }

    private Boolean getBooleanSetting(String key) {
        String setting = serviceExtensionContext.getSetting(key, null);
        return setting == null ? null : Boolean.parseBoolean(setting);
    }

    private String getStringSetting(String key) {
        return serviceExtensionContext.getSetting(key, null);
    }
}
