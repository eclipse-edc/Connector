/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api.management.configuration;

import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;

public class ManagementApiConfiguration extends WebServiceConfiguration {

    public ManagementApiConfiguration(String contextAlias) {
        super();
        this.contextAlias = contextAlias;
    }

    public ManagementApiConfiguration(WebServiceConfiguration webServiceConfiguration) {
        this.contextAlias = webServiceConfiguration.getContextAlias();
        this.path = webServiceConfiguration.getPath();
        this.port = webServiceConfiguration.getPort();
    }

}
