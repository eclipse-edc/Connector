/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *       Cofinity-X - extract presentation request service
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.iam.decentralizedclaims.core.validation.SelfIssueIdTokenValidationAction;
import org.eclipse.edc.iam.decentralizedclaims.service.DcpIdentityService;
import org.eclipse.edc.iam.decentralizedclaims.service.verification.MultiFormatPresentationVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.ClaimTokenCreatorFunction;
import org.eclipse.edc.iam.decentralizedclaims.spi.DcpParticipantAgentServiceExtension;
import org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.VerifiableCredentialValidationServiceImpl;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.jwt.Vcdm20JosePresentationVerifier;
import org.eclipse.edc.verifiablecredentials.jwt.rules.HasSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.JtiValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.SubJwkIsNullRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.TokenNotNullRule;
import org.eclipse.edc.verifiablecredentials.linkeddata.DidMethodResolver;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;

import java.net.URISyntaxException;
import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.STATUSLIST_2021_URL;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.JWT_VC_TOKEN_CONTEXT;

@Extension("DCP Core Extension")
public class DcpCoreExtension implements ServiceExtension {

    public static final String DCP_SELF_ISSUED_TOKEN_CONTEXT = "dcp-si";
    public static final String JSON_2020_SIGNATURE_SUITE = "JsonWebSignature2020";
    public static final long DEFAULT_CLEANUP_PERIOD_SECONDS = 60;

    @Setting(description = "DID of the participant, only needed if different from the value in edc.participant.id", required = false)
    public static final String PARTICIPANT_DID = "edc.participant.did";

    @Setting(description = "DEPRECATED: DID of the participant, please refer to " + PARTICIPANT_DID)
    @Deprecated(since = "0.17.0")
    public static final String DEPRECATED_ISSUER_ID_KEY = "edc.iam.issuer.id";

    @Setting(
            key = "edc.sql.store.jti.cleanup.period",
            description = "The period of the JTI entry reaper thread in seconds",
            defaultValue = DEFAULT_CLEANUP_PERIOD_SECONDS + "")
    private long reaperCleanupPeriod;

    @Setting(
            key = "edc.iam.accesstoken.jti.validation",
            description = "Activate or deactivate JTI validation",
            defaultValue = "true")
    private boolean activateJtiValidation;

    @Inject
    private SecureTokenService secureTokenService;
    @Inject
    private TrustedIssuerRegistry trustedIssuerRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private SignatureSuiteRegistry signatureSuiteRegistry;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private Clock clock;
    @Inject
    private DidResolverRegistry didResolverRegistry;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private TokenValidationRulesRegistry rulesRegistry;
    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;
    @Inject
    private ClaimTokenCreatorFunction claimTokenFunction;
    @Inject
    private ParticipantAgentService participantAgentService;
    @Inject(required = false)
    private DcpParticipantAgentServiceExtension participantAgentServiceExtension;
    @Inject
    private RevocationServiceRegistry revocationServiceRegistry;
    @Inject
    private ParticipantContextConfig participantContextConfig;
    @Inject
    private JtiValidationStore jtiValidationStore;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private PresentationRequestService presentationRequestService;

    private PresentationVerifier presentationVerifier;
    private ScheduledFuture<?> jtiEntryReaperThread;

    @Override
    public void initialize(ServiceExtensionContext context) {

        // add all rules for self-issued ID tokens
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new IssuerEqualsSubjectRule());
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new SubJwkIsNullRule());
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 5, false));
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new TokenNotNullRule());
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new NotBeforeValidationRule(clock, 4, true));

        // add all rules for validating VerifiableCredential JWTs
        rulesRegistry.addRule(JWT_VC_TOKEN_CONTEXT, new HasSubjectRule());

        if (activateJtiValidation) {
            rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new JtiValidationRule(jtiValidationStore, context.getMonitor()));
        }

        try {
            jsonLd.registerCachedDocument(STATUSLIST_2021_URL, getClass().getClassLoader().getResource("statuslist2021.json").toURI());
        } catch (URISyntaxException e) {
            context.getMonitor().warning("Could not load JSON-LD file", e);
        }

        if (participantAgentServiceExtension != null) {
            participantAgentService.register(participantAgentServiceExtension);
        }
    }

    @Override
    public void start() {
        if (activateJtiValidation) {
            jtiEntryReaperThread = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), "JTI Validation Entry Reaper Thread")
                    .scheduleAtFixedRate(jtiValidationStore::deleteExpired, reaperCleanupPeriod, reaperCleanupPeriod, TimeUnit.SECONDS);
        }
    }

    @Override
    public void shutdown() {
        if (jtiEntryReaperThread != null && !jtiEntryReaperThread.isCancelled()) {
            jtiEntryReaperThread.cancel(true);
        }
    }

    @Override
    public void prepare() {
        // TODO move in a separated extension?
        signatureSuiteRegistry.register(JSON_2020_SIGNATURE_SUITE, new Jws2020SignatureSuite(typeManager.getMapper(JSON_LD)));
    }

    @Provider
    public IdentityService createIdentityService(ServiceExtensionContext context) {
        var didConfigProvider = new DidConfigProvider(participantContextConfig, context.getMonitor());
        var validationAction = new SelfIssueIdTokenValidationAction(tokenValidationService, rulesRegistry, didPublicKeyResolver, didConfigProvider);

        var credentialValidationService = new VerifiableCredentialValidationServiceImpl(createPresentationVerifier(context),
                trustedIssuerRegistry, revocationServiceRegistry, clock, typeManager.getMapper());

        return new DcpIdentityService(secureTokenService, didConfigProvider, validationAction,
                presentationRequestService, claimTokenFunction, credentialValidationService);
    }

    @Provider
    public PresentationVerifier createPresentationVerifier(ServiceExtensionContext context) {
        if (presentationVerifier == null) {

            var jwtVerifier = new JwtPresentationVerifier(typeManager, JSON_LD, tokenValidationService, rulesRegistry, didPublicKeyResolver);
            var ldpVerifier = LdpVerifier.Builder.newInstance()
                    .signatureSuites(signatureSuiteRegistry)
                    .jsonLd(jsonLd)
                    .typeManager(typeManager)
                    .typeContext(JSON_LD)
                    .methodResolver(new DidMethodResolver(didResolverRegistry))
                    .build();

            var joseVerifier = new Vcdm20JosePresentationVerifier(tokenValidationService, didPublicKeyResolver);

            presentationVerifier = new MultiFormatPresentationVerifier(jwtVerifier, ldpVerifier, joseVerifier);
        }
        return presentationVerifier;
    }

}
