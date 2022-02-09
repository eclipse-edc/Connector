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
package org.eclipse.dataspaceconnector.dataplane.validation.server;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.dataplane.validation.server.controller.DataPlaneValidationFacadeController;
import org.eclipse.dataspaceconnector.dataplane.validation.server.rules.ContractValidationRule;
import org.eclipse.dataspaceconnector.dataplane.validation.server.rules.ExpirationDateValidationRule;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.token.validation.impl.TokenValidationServiceImpl;

import java.security.PublicKey;
import java.util.Arrays;

/**
 * Provides service to validate and decode tokens.
 */
public class DataPlaneValidationServerExtension implements ServiceExtension {

    @EdcSetting
    private static final String DATA_PLANE_VALIDATION_PUBLIC_KEY_ALIAS = "edc.dataplane.validation.public-key.alias";

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Override
    public String name() {
        return "Access Validation Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var validationRules = Arrays.asList(new ContractValidationRule(contractNegotiationStore), new ExpirationDateValidationRule());
        var tokenValidationService = new TokenValidationServiceImpl(extractPublicKey(context), validationRules);
        var webService = context.getService(WebService.class);
        webService.registerController(new DataPlaneValidationFacadeController(tokenValidationService));
    }

    private static PublicKey extractPublicKey(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var publicKeyId = context.getSetting(DATA_PLANE_VALIDATION_PUBLIC_KEY_ALIAS, null);
        if (publicKeyId == null) {
            throw new EdcException(String.format("Missing mandatory public key alias setting `%s`", DATA_PLANE_VALIDATION_PUBLIC_KEY_ALIAS));
        }
        var secret = vault.resolveSecret(publicKeyId);
        if (secret == null) {
            throw new EdcException("Failed to retrieve secret with id: " + publicKeyId);
        }

        try {
            ECKey jwk = (ECKey) JWK.parseFromPEMEncodedObjects(secret);
            return jwk.toRSAKey().toPublicKey();
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }
}
