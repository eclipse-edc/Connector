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

import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactoryConfig;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConnectionFactoryConfigFactoryTest {

    private static final int TOTAL_SETTINGS_COUNT = 17;

    private ConnectionFactoryConfigFactory factory;

    private Map<String, String> settingMap;

    // mocks
    private ServiceExtensionContext serviceExtensionContext;

    @BeforeEach
    public void setup() {
        settingMap = new HashMap<>();
        serviceExtensionContext = Mockito.mock(ServiceExtensionContext.class);
        Monitor monitor = new MyMonitor();

        Mockito.when(serviceExtensionContext.getMonitor()).thenReturn(monitor);
        Mockito.when(serviceExtensionContext.getSetting(Mockito.anyString(), Mockito.isNull()))
                .thenAnswer((a) -> {
                    String key = a.getArgument(0);
                    if (settingMap.containsKey(key)) {
                        return settingMap.get(key);
                    }
                    return null;
                });

        factory = new ConnectionFactoryConfigFactory(serviceExtensionContext);
    }

    @AfterEach
    public void tearDown() {
        Mockito.verify(serviceExtensionContext, Mockito.times(TOTAL_SETTINGS_COUNT))
                .getSetting(Mockito.anyString(), Mockito.isNull());
    }

    @Test
    public void testThrowsOnMissingUsername() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnMissingUrl() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidSslMode() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_SSL_MODE, "bar");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidSslCertPath() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_SSL_CERT, "/foo/bar");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidSslKeyPath() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_SSL_KEY, "/foo/bar");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidLoggerLevel() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_LOGGER_LEVEL, "foo");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidSocketTimeout() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_SOCKET_TIMEOUT, "foo");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidConnectionTimeout() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_TIMEOUT, "foo");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testThrowsOnInvalidLoginTimeout() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_LOGIN_TIMEOUT, "foo");

        // invoke & verify
        Assertions.assertThatThrownBy(() -> factory.create())
                .isInstanceOf(EdcException.class);
    }

    @Test
    public void testSuccess() {
        // prepare
        settingMap.put(SettingKeys.POSTGRESQL_URL, "https://example.com");
        settingMap.put(SettingKeys.POSTGRESQL_USERNAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_PASSWORD, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_SSL, "true");
        settingMap.put(SettingKeys.POSTGRESQL_SSL_MODE, "verify-ca");
        settingMap.put(SettingKeys.POSTGRESQL_SSL_CERT, System.getProperty("user.dir"));
        settingMap.put(SettingKeys.POSTGRESQL_SSL_KEY, System.getProperty("user.dir"));
        settingMap.put(SettingKeys.POSTGRESQL_SSL_ROOT_CERT, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_SSL_HOSTNAME_VERIFIER, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_LOGGER_LEVEL, "trace");
        settingMap.put(SettingKeys.POSTGRESQL_LOGGER_FILE, System.getProperty("user.dir"));
        settingMap.put(SettingKeys.POSTGRESQL_LOG_UNCLOSED_CONNECTION, "true");
        settingMap.put(SettingKeys.POSTGRESQL_SOCKET_TIMEOUT, "1");
        settingMap.put(SettingKeys.POSTGRESQL_CONNECTION_TIMEOUT, "1");
        settingMap.put(SettingKeys.POSTGRESQL_APPLICATION_NAME, "foo");
        settingMap.put(SettingKeys.POSTGRESQL_LOGIN_TIMEOUT, "1");
        settingMap.put(SettingKeys.POSTGRESQL_SSL_READONLY, "true");

        // invoke
        ConnectionFactoryConfig config = factory.create();

        // verify
        Assertions.assertThat(config.getUri()).isEqualTo(URI.create("https://example.com"));
        Assertions.assertThat(config.getUserName()).isEqualTo("foo");
        Assertions.assertThat(config.getPassword()).isEqualTo("foo");
        Assertions.assertThat(config.getSsl()).isTrue();
        Assertions.assertThat(config.getSslMode().toString()).isEqualTo(ConnectionFactoryConfig.SslMode.VERIFY_CA.toString());
        Assertions.assertThat(config.getSslCert()).isEqualTo(Path.of(System.getProperty("user.dir")));
        Assertions.assertThat(config.getSslKey()).isEqualTo(Path.of(System.getProperty("user.dir")));
        Assertions.assertThat(config.getSslRootCert()).isEqualTo(Path.of("foo"));
        Assertions.assertThat(config.getSslHostNameVerifier()).isEqualTo("foo");
        Assertions.assertThat(config.getLoggerLevel().toString()).isEqualTo(ConnectionFactoryConfig.LoggerLevel.TRACE.toString());
        Assertions.assertThat(config.getLoggerFile()).isEqualTo(Path.of(System.getProperty("user.dir")));
        Assertions.assertThat(config.getLogUnclosedConnection()).isTrue();
        Assertions.assertThat(config.getSocketTimeout()).isEqualTo(1);
        Assertions.assertThat(config.getConnectTimeout()).isEqualTo(1);
        Assertions.assertThat(config.getApplicationName()).isEqualTo("foo");
        Assertions.assertThat(config.getLoginTimeout()).isEqualTo(1);
        Assertions.assertThat(config.getReadOnly()).isTrue();
    }

    private static class MyMonitor implements Monitor {
        @Override
        public void severe(String message, Throwable... errors) {
            System.out.println(message);
        }
    }
}
