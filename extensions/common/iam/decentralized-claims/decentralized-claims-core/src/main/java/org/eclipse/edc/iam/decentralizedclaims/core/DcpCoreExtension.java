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

import org.eclipse.edc.iam.decentralizedclaims.core.discovery.DidDiscoveryUrlResolver;
import org.eclipse.edc.iam.decentralizedclaims.core.validation.SelfIssueIdTokenValidationAction;
import org.eclipse.edc.iam.decentralizedclaims.service.DcpIdentityService;
import org.eclipse.edc.iam.decentralizedclaims.service.verification.MultiFormatPresentationVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.ClaimTokenCreatorFunction;
import org.eclipse.edc.iam.decentralizedclaims.spi.DcpParticipantAgentServiceExtension;
import org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifierRegistry;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.VerifiableCredentialValidationServiceImpl;
import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidSchema;
import org.eclipse.edc.iam.verifiablecredentials.rules.IsInValidityPeriod;
import org.eclipse.edc.iam.verifiablecredentials.rules.IsNotRevoked;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigEntry;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantContextConfigValidatorRegistry;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.rules.ClaimIsPresentRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.HasSubjectRule;
import org.eclipse.edc.token.rules.JtiUnusedValidationRule;
import org.eclipse.edc.token.rules.JtiValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.jwt.Vcdm20JosePresentationVerifier;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.SubJwkIsNullRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.TokenNotNullRule;

import java.net.URISyntaxException;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.STATUSLIST_2021_URL;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.notBlank;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.startsWith;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.JWT_VC_TOKEN_CONTEXT;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.JWT_VP_TOKEN_CONTEXT;

@Extension("DCP Core Extension")
public class DcpCoreExtension implements ServiceExtension {

    public static final String DCP_SELF_ISSUED_TOKEN_CONTEXT = "dcp-si";
    /**
     * The JsonWebSignature2020 suite is only used to verify Linked-Data presentations, which are not supported by
     * default anymore.
     *
     * @deprecated the suite is registered by the DCP LDP verifier extension.
     */
    @Deprecated(since = "1.0.0")
    public static final String JSON_2020_SIGNATURE_SUITE = "JsonWebSignature2020";
    public static final long DEFAULT_CLEANUP_PERIOD_SECONDS = 60;

    @Setting(description = "DID of the participant, only needed if different from the value in edc.participant.id", required = false)
    public static final String PARTICIPANT_DID = "edc.participant.did";

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

    // no hyphens in the key, so that it can be set as an environment variable (EDC_IAM_ACCESSTOKEN_CLAIMS_REQUIRED)
    @Setting(
            key = "edc.iam.accesstoken.claims.required",
            description = "If true (default), self-issued ID tokens without a 'jti' or 'iat' claim are rejected, as required by the DCP specification. " +
                    "Set to false to accept them from counter-parties whose token issuer does not set these claims.",
            defaultValue = "true")
    private boolean requireJtiAndIat;

    @Setting(
            key = "edc.iam.presentation.binding.required",
            description = "Rejects Verifiable Presentations that are not bound to an audience and an expiry: JWT presentations " +
                    "without 'exp' and VCDM 2.0 presentations without 'aud', Linked-Data presentations without a proof 'domain' and 'expires'. " +
                    "If disabled, these are only checked when present.",
            defaultValue = "false")
    private boolean requirePresentationBinding;

    @Setting(
            key = "edc.iam.vc.validation.schema.subject-only",
            description = "When set to true, the JSON schema referenced in a VC will be validated only " +
                    "against the credential subjects instead of the credential itself.",
            defaultValue = "false"
    )
    private boolean validateOnlyCredentialSubject;

    @Setting(
            key = "edc.iam.vc.validation.issuer.types.match-any",
            description = "If true, a credential is accepted when any of its types is supported by its trusted issuer. " +
                    "If false (default), the trusted issuer must support every type of the credential, except the base 'VerifiableCredential' type.",
            defaultValue = "false"
    )
    private boolean matchAnyIssuerType;

    @Inject
    private SecureTokenService secureTokenService;
    @Inject
    private TypeManager typeManager;
    @Inject
    private CredentialVerifierRegistry credentialVerifierRegistry;
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
    private ParticipantContextConfigValidatorRegistry configValidatorRegistry;
    @Inject
    private JtiValidationStore jtiValidationStore;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private PresentationRequestService presentationRequestService;
    @Inject
    private DiscoveryService discoveryService;

    private PresentationVerifier presentationVerifier;
    private ScheduledFuture<?> jtiEntryReaperThread;
    private Monitor monitor;


    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        discoveryService.registerResolver(new DidDiscoveryUrlResolver(didResolverRegistry));

        // the participant DID is resolved from either of the two keys, see DidConfigProvider
        configValidatorRegistry.register(ParticipantConfigEntry.Builder.newInstance(DidConfigProvider.PARTICIPANT_ID)
                .description("ID of the participant, used as its DID")
                .validator(notBlank())
                .build());
        configValidatorRegistry.register(ParticipantConfigEntry.Builder.newInstance(PARTICIPANT_DID)
                .description("DID of the participant, required if %s is not set".formatted(DidConfigProvider.PARTICIPANT_ID))
                .validator(startsWith("did:"))
                .requiredWhen(config -> !config.hasKey(DidConfigProvider.PARTICIPANT_ID))
                .build());

        // add all rules for self-issued ID tokens
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new IssuerEqualsSubjectRule());
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new SubJwkIsNullRule());
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 5, false));
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new TokenNotNullRule());
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new NotBeforeValidationRule(clock, 4, true));
        if (requireJtiAndIat) {
            // both claims are mandatory in a self-issued ID token. Without a 'jti', a token would not be checked for replays
            rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new ClaimIsPresentRule(JWT_ID));
            rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new ClaimIsPresentRule(ISSUED_AT));
        }

        // add all rules for validating VerifiableCredential JWTs
        rulesRegistry.addRule(JWT_VC_TOKEN_CONTEXT, new HasSubjectRule());
        rulesRegistry.addRule(JWT_VC_TOKEN_CONTEXT, new IssuerKeyIdValidationRule());

        // add all rules for validating VerifiablePresentation JWTs
        rulesRegistry.addRule(JWT_VP_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 5, !requirePresentationBinding));
        rulesRegistry.addRule(JWT_VP_TOKEN_CONTEXT, new NotBeforeValidationRule(clock, 5, true));

        if (activateJtiValidation) {
            // the JTI is only checked here, and recorded once the whole verification succeeded (see createIdentityService),
            // so that a request whose verification could not be completed can be retried with the same token
            rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new JtiUnusedValidationRule(jtiValidationStore, context.getMonitor()));
        }

        try {
            jsonLd.registerCachedDocument(STATUSLIST_2021_URL, getClass().getClassLoader().getResource("statuslist2021.json").toURI());
        } catch (URISyntaxException e) {
            context.getMonitor().warning("Could not load JSON-LD file", e);
        }

        if (participantAgentServiceExtension != null) {
            participantAgentService.register(participantAgentServiceExtension);
        }

        // the supported presentation formats: VCDM 1.1 JWT and VCDM 2.0 JOSE. Other formats, e.g. Linked-Data
        // presentations, are only accepted if an extension registers a verifier for them
        credentialVerifierRegistry.register(new JwtPresentationVerifier(typeManager, JSON_LD, tokenValidationService, rulesRegistry, didPublicKeyResolver));
        credentialVerifierRegistry.register(new Vcdm20JosePresentationVerifier(monitor, tokenValidationService, rulesRegistry, didPublicKeyResolver, requirePresentationBinding));
    }

    @Override
    public void start() {
        if (activateJtiValidation) {
            jtiEntryReaperThread = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), "JTI Validation Entry Reaper Thread")
                    .scheduleAtFixedRate(this::deleteExpiredJtiEntries, reaperCleanupPeriod, reaperCleanupPeriod, TimeUnit.SECONDS);
        }
    }

    @Override
    public void shutdown() {
        if (jtiEntryReaperThread != null && !jtiEntryReaperThread.isCancelled()) {
            jtiEntryReaperThread.cancel(true);
        }
    }

    private void deleteExpiredJtiEntries() {
        // an exception would end the periodic execution, e.g. when the database is not reachable for a moment
        try {
            jtiValidationStore.deleteExpired()
                    .onFailure(f -> monitor.warning("Failed to delete expired JTI entries: %s".formatted(f.getFailureDetail())));
        } catch (RuntimeException e) {
            monitor.warning("Failed to delete expired JTI entries", e);
        }
    }

    @Provider
    public IdentityService createIdentityService(ServiceExtensionContext context) {
        var didConfigProvider = new DidConfigProvider(participantContextConfig, context.getMonitor());
        var validationAction = new SelfIssueIdTokenValidationAction(tokenValidationService, rulesRegistry, didPublicKeyResolver, didConfigProvider, didResolverRegistry);

        var rules = List.of(
                new IsInValidityPeriod(clock),
                new IsNotRevoked(revocationServiceRegistry),
                new HasValidSchema(typeManager.getMapper(), validateOnlyCredentialSubject)
        );

        var credentialValidationService = new VerifiableCredentialValidationServiceImpl(createPresentationVerifier(context), rules, matchAnyIssuerType);

        TokenValidationRule verifiedTokenRule = activateJtiValidation ?
                new JtiValidationRule(jtiValidationStore, context.getMonitor()) :
                (toVerify, additional) -> Result.success();

        return new DcpIdentityService(secureTokenService, didConfigProvider, validationAction,
                presentationRequestService, claimTokenFunction, credentialValidationService, verifiedTokenRule);
    }

    @Provider
    public PresentationVerifier createPresentationVerifier(ServiceExtensionContext context) {
        if (presentationVerifier == null) {
            presentationVerifier = new MultiFormatPresentationVerifier(credentialVerifierRegistry);
        }
        return presentationVerifier;
    }

}
