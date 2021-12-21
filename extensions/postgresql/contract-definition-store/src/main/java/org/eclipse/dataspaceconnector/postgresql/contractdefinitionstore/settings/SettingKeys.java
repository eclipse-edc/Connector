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

package org.eclipse.dataspaceconnector.postgresql.contractdefinitionstore.settings;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public class SettingKeys {
    @EdcSetting
    public static final String POSTGRESQL_URL = "edc.postgresql.url";
    @EdcSetting
    public static final String POSTGRESQL_USERNAME = "edc.postgresql.username";
    @EdcSetting
    public static final String POSTGRESQL_PASSWORD = "edc.postgresql.password";
    @EdcSetting
    public static final String POSTGRESQL_SSL = "edc.postgresql.ssl";
    @EdcSetting
    public static final String POSTGRESQL_SSL_MODE = "edc.postgresql.sslMode";
    @EdcSetting
    public static final String POSTGRESQL_SSL_CERT = "edc.postgresql.sslCert";
    @EdcSetting
    public static final String POSTGRESQL_SSL_KEY = "edc.postgresql.sslKey";
    @EdcSetting
    public static final String POSTGRESQL_SSL_ROOT_CERT = "edc.postgresql.sslRootCert";
    @EdcSetting
    public static final String POSTGRESQL_SSL_READONLY = "edc.postgresql.readonly";
    @EdcSetting
    public static final String POSTGRESQL_SSL_HOSTNAME_VERIFIER = "edc.postgresql.ssl.hostnameVerifier";
    @EdcSetting
    public static final String POSTGRESQL_LOGGER_LEVEL = "edc.postgresql.loggerLevel";
    @EdcSetting
    public static final String POSTGRESQL_LOGGER_FILE = "edc.postgresql.loggerFile";
    @EdcSetting
    public static final String POSTGRESQL_LOG_UNCLOSED_CONNECTION = "edc.postgresql.logUnclosedConnection";
    @EdcSetting
    public static final String POSTGRESQL_SOCKET_TIMEOUT = "edc.postgresql.socketTimeout";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_TIMEOUT = "edc.postgresql.connectionTimeout";
    @EdcSetting
    public static final String POSTGRESQL_LOGIN_TIMEOUT = "edc.postgresql.loginTimeout";
    @EdcSetting
    public static final String POSTGRESQL_APPLICATION_NAME = "edc.postgresql.applicationName";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_POOL_MAX_IDLE_CONNECTIONS = "edc.postgresql.connection.pool.max.idle.connections";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_POOL_MIN_IDLE_CONNECTIONS = "edc.postgresql.connection.pool.min.idle.connections";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_POOL_MAX_TOTAL_CONNECTIONS = "edc.postgresql.connection.pool.max.total.connections";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_BORROW = "edc.postgresql.connection.pool.test.connection.on.borrow";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_CREATE = "edc.postgresql.connection.pool.test.connection.on.create";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_ON_RETURN = "edc.postgresql.connection.pool.test.connection.on.return";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_WHILE_IDLE = "edc.postgresql.connection.pool.test.connection.while.idle";
    @EdcSetting
    public static final String POSTGRESQL_CONNECTION_POOL_TEST_CONNECTION_QUERY = "edc.postgresql.connection.pool.test.connection.query";
}
