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

package org.eclipse.dataspaceconnector.extension.jetty;

public class JettyConfiguration {

    private final int httpPort;
    private final String keystorePassword;
    private final String keymanagerPassword;

    public JettyConfiguration(int httpPort, String keystorePassword, String keymanagerPassword) {
        this.httpPort = httpPort;
        this.keystorePassword = keystorePassword;
        this.keymanagerPassword = keymanagerPassword;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeymanagerPassword() {
        return keymanagerPassword;
    }
}
