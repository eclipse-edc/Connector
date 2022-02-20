/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.dataspaceconnector.transfer.sync.api;

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
import org.eclipse.dataspaceconnector.transfer.sync.api.controller.SyncDataTransferValidationApiController;
import org.eclipse.dataspaceconnector.transfer.sync.api.resolver.SelfPublicKeyResolver;
import org.eclipse.dataspaceconnector.transfer.sync.api.rules.ContractValidationRule;
import org.eclipse.dataspaceconnector.transfer.sync.api.rules.ExpirationDateValidationRule;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class SyncDataTransferValidationApiExtension implements ServiceExtension {

    private static final String VALIDATION = "validation";

    @EdcSetting
    private static final String CONTROL_PLANE_PUBLIC_KEY_ALIAS = "edc.transfer.sync.public-key.alias";

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Override

    public String name() {
        return "Sync Data Transfer Validation API";
    }

    /**
     * Register Api that is called by data plane in order to validate and decode tokens.
     */
    @Override
    public void initialize(ServiceExtensionContext context) {
        var validationRules = Arrays.asList(new ContractValidationRule(contractNegotiationStore), new ExpirationDateValidationRule());
        var resolver = createResolver(context);
        var tokenValidationService = new TokenValidationServiceImpl(resolver, validationRules);
        var webService = context.getService(WebService.class);
        webService.registerResource(VALIDATION, new SyncDataTransferValidationApiController(context.getMonitor(), tokenValidationService));
    }

    private static PublicKeyResolver createResolver(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var publicKeyAlias = getMandatorySetting(context, CONTROL_PLANE_PUBLIC_KEY_ALIAS);
        return new SelfPublicKeyResolver(vault, publicKeyAlias);
    }

    private static @NotNull String getMandatorySetting(ServiceExtensionContext context, String name) {
        var setting = context.getSetting(name, null);
        if (setting == null) {
            throw new EdcException(String.format("Missing mandatory setting `%s`", name));
        }
        return setting;
    }
}
