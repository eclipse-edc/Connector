/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.oauth2.tokenexchange;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.signaling.oauth2.tokenexchange.logic.FileWorkloadCredentialSource;
import org.eclipse.edc.signaling.oauth2.tokenexchange.logic.Oauth2TokenExchangeSignalingAuthorization;
import org.eclipse.edc.signaling.oauth2.tokenexchange.logic.WorkloadCredentialSource;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.ClaimIsPresentRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.HasSubjectRule;
import org.eclipse.edc.token.rules.JtiValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.time.Clock;

import static org.eclipse.edc.signaling.oauth2.tokenexchange.DataPlaneSignalingOauth2TokenExchangeExtension.NAME;

/**
 * Registers the {@code oauth2_token_exchange} Data Plane Signaling authorization profile.
 */
@Extension(value = NAME)
public class DataPlaneSignalingOauth2TokenExchangeExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling OAuth2 Token Exchange";

    public static final String VALIDATION_RULES_CONTEXT = "signaling-api-oauth2-token-exchange";
    static final String DEFAULT_AUDIENCE = "edcv";


    @Setting(description = "Path to the file holding the workload credential (e.g. a projected Kubernetes ServiceAccount token) presented to the token exchange broker as the subject token",
            key = "edc.dps.oauth2.tokenexchange.subjecttokenpath", defaultValue = "/var/run/secrets/kubernetes.io/serviceaccount/token")
    private String subjectTokenPath;

    @Setting(description = "The scope requested for the exchanged token, used when the authorization profile does not carry one",
            key = "edc.dps.oauth2.tokenexchange.scope", required = false)
    private String scope;

    @Setting(description = "The audience requested for the exchanged token. Must match the token-exchange service's configured audience.",
            key = "edc.dps.oauth2.tokenexchange.audience", defaultValue = DEFAULT_AUDIENCE)
    private String audience;

    @Inject
    private SignalingAuthorizationRegistry signalingAuthorizationRegistry;
    @Inject
    private Oauth2Client oauth2Client;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;
    @Inject
    private KeyParserRegistry keyParserRegistry;
    @Inject
    private JtiValidationStore jtiValidationStore;
    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        tokenValidationRulesRegistry.addRule(VALIDATION_RULES_CONTEXT, new HasSubjectRule());
        tokenValidationRulesRegistry.addRule(VALIDATION_RULES_CONTEXT, new JtiValidationRule(jtiValidationStore, monitor.withPrefix(VALIDATION_RULES_CONTEXT)));
        tokenValidationRulesRegistry.addRule(VALIDATION_RULES_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 0, false));
        tokenValidationRulesRegistry.addRule(VALIDATION_RULES_CONTEXT, new ClaimIsPresentRule("scope"));

        signalingAuthorizationRegistry.register(new Oauth2TokenExchangeSignalingAuthorization(oauth2Client,
                tokenValidationService, tokenValidationRulesRegistry, keyParserRegistry, workloadCredentialSource(),
                monitor, audience, scope));
    }

    private WorkloadCredentialSource workloadCredentialSource() {
        if (subjectTokenPath == null) {
            return () -> Result.failure("No workload credential configured: set 'edc.dps.oauth2.tokenexchange.subjecttokenpath' to use the '%s' authorization profile for outbound signaling requests"
                    .formatted(Oauth2TokenExchangeSignalingAuthorization.TYPE));
        }
        return new FileWorkloadCredentialSource(subjectTokenPath);
    }
}
