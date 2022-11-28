/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.control.configuration;

import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;

public class ControlApiConfiguration extends WebServiceConfiguration {

    public ControlApiConfiguration(String contextAlias) {
        super();
        this.contextAlias = contextAlias;
    }

    public ControlApiConfiguration(WebServiceConfiguration webServiceConfiguration) {
        this.contextAlias = webServiceConfiguration.getContextAlias();
        this.path = webServiceConfiguration.getPath();
        this.port = webServiceConfiguration.getPort();
    }

}
