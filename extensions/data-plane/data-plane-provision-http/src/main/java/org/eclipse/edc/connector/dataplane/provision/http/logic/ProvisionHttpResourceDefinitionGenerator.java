/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.provision.http.logic;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;

import static org.eclipse.edc.connector.dataplane.provision.http.logic.ProvisionHttp.PROVISION_HTTP_TYPE;

public class ProvisionHttpResourceDefinitionGenerator implements ResourceDefinitionGenerator {

    @Override
    public String supportedType() {
        return PROVISION_HTTP_TYPE;
    }

    @Override
    public ProvisionResource generate(DataFlow dataFlow) {
        var baseUrl = dataFlow.getSource().getProperty("baseUrl");
        if (baseUrl == null) {
            throw new IllegalArgumentException("`baseUrl` property is mandatory for %s type".formatted(PROVISION_HTTP_TYPE));
        }
        return ProvisionResource.Builder.newInstance()
                .flowId(dataFlow.getId())
                .type(PROVISION_HTTP_TYPE)
                .property("endpoint", baseUrl)
                .build();
    }
}
