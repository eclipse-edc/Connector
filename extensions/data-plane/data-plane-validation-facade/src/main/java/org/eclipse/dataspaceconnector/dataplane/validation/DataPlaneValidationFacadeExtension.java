/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataplane.validation;

import org.eclipse.dataspaceconnector.dataplane.validation.controller.DataPlaneValidationFacadeController;
import org.eclipse.dataspaceconnector.dataplane.validation.resolver.VaultPublicKeyResolver;
import org.eclipse.dataspaceconnector.dataplane.validation.rules.ContractValidationRule;
import org.eclipse.dataspaceconnector.dataplane.validation.rules.ExpirationDateValidationRule;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.token.TokenValidationServiceImpl;

import java.util.Arrays;

/**
 * Provides service to validate and decode tokens.
 */
public class DataPlaneValidationFacadeExtension implements ServiceExtension {

    private static final String VALIDATION = "validation";

    @EdcSetting
    private static final String CONTROL_PLANE_PUBLIC_KEY_ALIAS = "edc.controlplane.public-key.alias";

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Override
    public String name() {
        return "Data Plane Validation Facade";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var validationRules = Arrays.asList(new ContractValidationRule(contractNegotiationStore), new ExpirationDateValidationRule());
        var resolver = createResolver(context);
        var tokenValidationService = new TokenValidationServiceImpl(resolver, validationRules);
        var webService = context.getService(WebService.class);
        webService.registerResource(VALIDATION, new DataPlaneValidationFacadeController(context.getMonitor(), tokenValidationService));
    }

    private static PublicKeyResolver createResolver(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var publicKeyAlias = context.getSetting(CONTROL_PLANE_PUBLIC_KEY_ALIAS, null);
        if (publicKeyAlias == null) {
            throw new EdcException(String.format("Missing mandatory setting `%s`", CONTROL_PLANE_PUBLIC_KEY_ALIAS));
        }
        return new VaultPublicKeyResolver(vault, publicKeyAlias);
    }
}
