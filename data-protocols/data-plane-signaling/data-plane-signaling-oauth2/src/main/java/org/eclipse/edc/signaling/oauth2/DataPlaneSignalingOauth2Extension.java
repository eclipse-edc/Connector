/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.signaling.oauth2;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.signaling.oauth2.logic.Oauth2CredentialsSignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.HasSubjectRule;
import org.eclipse.edc.token.rules.JtiValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.time.Clock;

public class DataPlaneSignalingOauth2Extension implements ServiceExtension {

    public static final String VALIDATION_RULES_CONTEXT = "signaling-api-oauth2";

    @Inject
    private SignalingAuthorizationRegistry signalingAuthorizationRegistry;
    @Inject
    private Oauth2Client oauth2Client;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;
    @Inject
    private Clock clock;
    @Inject
    private JtiValidationStore jtiValidationStore;
    @Inject
    private KeyParserRegistry keyParserRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        tokenValidationRulesRegistry.addRule(VALIDATION_RULES_CONTEXT, new HasSubjectRule());
        tokenValidationRulesRegistry.addRule(VALIDATION_RULES_CONTEXT, new JtiValidationRule(jtiValidationStore, context.getMonitor().withPrefix(VALIDATION_RULES_CONTEXT)));
        tokenValidationRulesRegistry.addRule(VALIDATION_RULES_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 0, false));

        signalingAuthorizationRegistry.register(new Oauth2CredentialsSignalingAuthorization(oauth2Client, tokenValidationService, tokenValidationRulesRegistry, keyParserRegistry, context.getMonitor()));
    }
}
