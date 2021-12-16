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

package org.eclipse.dataspaceconnector.clients.postgresql.connection;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * A ConnectionFactoryConfig is a container object containing a set of connection configuration
 * parameters that can be used by a ConnectionFactory for connection creation.
 */
public class ConnectionFactoryConfig {
    /**
     * The jdbc url to connect to
     */
    private final URI uri;
    private final String userName;
    private final String password;
    private final Boolean ssl;
    private final SslMode sslMode;
    private final Path sslCert;
    private final Path sslKey;
    private final Path sslRootCert;
    private final String sslHostNameVerifier;
    private final LoggerLevel loggerLevel;
    private final Path loggerFile;
    private final Boolean logUnclosedConnection;
    private final long socketTimeout;
    private final long connectTimeout;
    private final long loginTimeout;
    private final String applicationName;
    private final boolean readOnly;
    private final boolean autoCommit;

    public ConnectionFactoryConfig(
            @NotNull URI uri,
            @NotNull String userName,
            String password,
            Boolean ssl,
            SslMode sslMode,
            Path sslCert,
            Path sslKey,
            Path sslRootCert,
            String sslHostNameVerifier,
            LoggerLevel loggerLevel,
            Path loggerFile,
            Boolean logUnclosedConnection,
            long socketTimeout,
            long connectTimeout,
            long loginTimeout,
            String applicationName,
            boolean readOnly,
            boolean autoCommit) {
        this.uri = Objects.requireNonNull(uri);
        this.userName = Objects.requireNonNull(userName);
        this.password = password;
        this.ssl = ssl;
        this.sslMode = sslMode;
        this.sslCert = sslCert;
        this.sslKey = sslKey;
        this.sslRootCert = sslRootCert;
        this.sslHostNameVerifier = sslHostNameVerifier;
        this.loggerLevel = loggerLevel;
        this.loggerFile = loggerFile;
        this.logUnclosedConnection = logUnclosedConnection;
        this.socketTimeout = socketTimeout;
        this.connectTimeout = connectTimeout;
        this.loginTimeout = loginTimeout;
        this.applicationName = applicationName;
        this.readOnly = readOnly;
        this.autoCommit = autoCommit;
    }

    public URI getUri() {
        return uri;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public Path getSslRootCert() {
        return sslRootCert;
    }

    public String getSslHostNameVerifier() {
        return sslHostNameVerifier;
    }

    public LoggerLevel getLoggerLevel() {
        return loggerLevel;
    }

    public Path getLoggerFile() {
        return loggerFile;
    }

    public long getSocketTimeout() {
        return socketTimeout;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public long getLoginTimeout() {
        return loginTimeout;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Boolean getSsl() {
        return ssl;
    }

    public SslMode getSslMode() {
        return sslMode;
    }

    public Path getSslCert() {
        return sslCert;
    }

    public Path getSslKey() {
        return sslKey;
    }

    public Boolean getLogUnclosedConnection() {
        return logUnclosedConnection;
    }

    public boolean getAutoCommit() {
        return autoCommit;
    }

    /**
     * Consult https://www.postgresql.org/docs/current/libpq-ssl.html
     */
    public enum SslMode {
        DISABLE("disable"),
        ALLOW("allow"),
        PREFER("prefer"),
        REQUIRE("require"),
        VERIFY_CA("verify-ca"),
        VERIFY_FULL("verify-full");
        private final String value;

        SslMode(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    public enum LoggerLevel {
        OFF,
        DEBUG,
        TRACE
    }

    public static class Builder {
        private URI uri;
        private String userName;
        private String password;
        private Boolean ssl;
        private SslMode sslMode = SslMode.PREFER;
        private Path sslCert;
        private Path sslKey;
        private Path sslRootCert;
        private String sslHostNameVerifier;
        private LoggerLevel loggerLevel;
        private Path loggerFile;
        private Boolean logUnclosedConnection;
        private long socketTimeout = Duration.ofMillis(5000).toMillis();
        private long connectTimeout = Duration.ofMillis(5000).toMillis();
        private long loginTimeout = Duration.ofMillis(5000).toMillis();
        private String applicationName;
        private boolean readOnly = false;
        private boolean autoCommit = false;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder uri(URI value) {
            this.uri = value;
            return this;
        }

        public Builder userName(String value) {
            this.userName = value;
            return this;
        }

        public Builder password(String value) {
            this.password = value;
            return this;
        }

        public Builder sslRootCert(Path value) {
            this.sslRootCert = value;
            return this;
        }

        public Builder sslHostNameVerifier(String value) {
            this.sslHostNameVerifier = value;
            return this;
        }

        public Builder loggerLevel(LoggerLevel value) {
            this.loggerLevel = value;
            return this;
        }

        public Builder loggerFile(Path value) {
            this.loggerFile = value;
            return this;
        }

        public Builder socketTimeout(Long value) {
            this.socketTimeout = value;
            return this;
        }

        public Builder connectTimeout(Long value) {
            this.connectTimeout = value;
            return this;
        }

        public Builder loginTimeout(Long value) {
            this.loginTimeout = value;
            return this;
        }

        public Builder applicationName(String value) {
            this.applicationName = value;
            return this;
        }

        public Builder readOnly(Boolean value) {
            this.readOnly = value;
            return this;
        }

        public Builder ssl(Boolean value) {
            this.ssl = value;
            return this;
        }

        public Builder sslMode(SslMode value) {
            this.sslMode = value;
            return this;
        }

        public Builder sslCert(Path value) {
            this.sslCert = value;
            return this;
        }

        public Builder sslKey(Path value) {
            this.sslKey = value;
            return this;
        }

        public Builder logUnclosedConnections(Boolean value) {
            this.logUnclosedConnection = value;
            return this;
        }

        public Builder autoCommit(Boolean value) {
            this.autoCommit = value;
            return this;
        }

        public ConnectionFactoryConfig build() {
            return new ConnectionFactoryConfig(
                    uri,
                    userName,
                    password,
                    ssl,
                    sslMode,
                    sslCert,
                    sslKey,
                    sslRootCert,
                    sslHostNameVerifier,
                    loggerLevel,
                    loggerFile,
                    logUnclosedConnection,
                    socketTimeout,
                    connectTimeout,
                    loginTimeout,
                    applicationName,
                    readOnly,
                    autoCommit
            );
        }
    }
}
