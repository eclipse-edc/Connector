/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.protocolversion;

import org.eclipse.edc.connector.controlplane.api.management.protocolversion.transform.JsonObjectToProtocolVersionRequestTransformer;
import org.eclipse.edc.connector.controlplane.api.management.protocolversion.v4alpha.ProtocolVersionApiV4AlphaController;
import org.eclipse.edc.connector.controlplane.api.management.protocolversion.validation.ProtocolVersionRequestValidator;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest.PROTOCOL_VERSION_REQUEST_TYPE;

@Extension(value = ProtocolVersionApiExtension.NAME)
public class ProtocolVersionApiExtension implements ServiceExtension {

    public static final String NAME = "Management API: Protocol Version";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private VersionService service;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var managementApiTransformerRegistry = transformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        managementApiTransformerRegistry.register(new JsonObjectToProtocolVersionRequestTransformer());

        webService.registerResource(ApiContext.MANAGEMENT, new ProtocolVersionApiV4AlphaController(service, managementApiTransformerRegistry, validatorRegistry));
        validatorRegistry.register(PROTOCOL_VERSION_REQUEST_TYPE, ProtocolVersionRequestValidator.instance());

    }
}
