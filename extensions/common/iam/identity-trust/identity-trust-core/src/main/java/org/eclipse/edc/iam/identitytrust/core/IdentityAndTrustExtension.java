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
 *
 */

package org.eclipse.edc.iam.identitytrust.core;

import jakarta.json.Json;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.core.defaults.DefaultCredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.service.DidCredentialServiceUrlResolver;
import org.eclipse.edc.iam.identitytrust.service.IdentityAndTrustService;
import org.eclipse.edc.iam.identitytrust.service.verification.MultiFormatPresentationVerifier;
import org.eclipse.edc.iam.identitytrust.spi.ClaimTokenCreatorFunction;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.spi.DcpParticipantAgentServiceExtension;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.identitytrust.spi.validation.TokenValidationAction;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.VerifiableCredentialValidationServiceImpl;
import org.eclipse.edc.iam.verifiablecredentials.revocation.bitstring.BitstringStatusListRevocationService;
import org.eclipse.edc.iam.verifiablecredentials.revocation.statuslist2021.StatusList2021RevocationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Status;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
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
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.jwt.rules.HasSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.SubJwkIsNullRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.TokenNotNullRule;
import org.eclipse.edc.verifiablecredentials.linkeddata.DidMethodResolver;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.STATUSLIST_2021_URL;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier.JWT_VC_TOKEN_CONTEXT;

@Extension("Identity And Trust Extension")
public class IdentityAndTrustExtension implements ServiceExtension {

    public static final long DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS = 15 * 60 * 1000L;
    public static final String DCP_SELF_ISSUED_TOKEN_CONTEXT = "dcp-si";
    public static final String JSON_2020_SIGNATURE_SUITE = "JsonWebSignature2020";
    public static final long DEFAULT_CLEANUP_PERIOD_SECONDS = 60;
    @Setting(description = "Validity period of cached StatusList2021 credential entries in milliseconds.", defaultValue = DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS + "", key = "edc.iam.credential.revocation.cache.validity")
    private long revocationCacheValidity;
    @Setting(description = "DID of this connector", key = "edc.iam.issuer.id")
    private String issuerId;
    @Setting(description = "The period of the JTI entry reaper thread in seconds", defaultValue = DEFAULT_CLEANUP_PERIOD_SECONDS + "", key = "edc.sql.store.jti.cleanup.period")
    private long reaperCleanupPeriod;
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
    private EdcHttpClient httpClient;

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

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

    @Inject
    private DcpParticipantAgentServiceExtension participantAgentServiceExtension;

    @Inject
    private RevocationServiceRegistry revocationServiceRegistry;

    @Inject
    private JtiValidationStore jtiValidationStore;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    private PresentationVerifier presentationVerifier;
    private CredentialServiceClient credentialServiceClient;
    private ScheduledFuture<?> jtiEntryReaperThread;

    @Override
    public void initialize(ServiceExtensionContext context) {

        // add all rules for self-issued ID tokens
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new IssuerEqualsSubjectRule());
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new SubJwkIsNullRule());
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new AudienceValidationRule(issuerId));
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, 5, false));
        rulesRegistry.addRule(DCP_SELF_ISSUED_TOKEN_CONTEXT, new TokenNotNullRule());

        // add all rules for validating VerifiableCredential JWTs
        rulesRegistry.addRule(JWT_VC_TOKEN_CONTEXT, new HasSubjectRule());


        try {
            jsonLd.registerCachedDocument(STATUSLIST_2021_URL, getClass().getClassLoader().getResource("statuslist2021.json").toURI());
        } catch (URISyntaxException e) {
            context.getMonitor().warning("Could not load JSON-LD file", e);
        }

        participantAgentService.register(participantAgentServiceExtension);

        // register revocation services
        revocationServiceRegistry.addService(StatusList2021Status.TYPE, new StatusList2021RevocationService(typeManager.getMapper(), revocationCacheValidity));
        revocationServiceRegistry.addService(BitstringStatusListStatus.TYPE, new BitstringStatusListRevocationService(typeManager.getMapper(), revocationCacheValidity));
    }

    @Override
    public void start() {
        jtiEntryReaperThread = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), "JTI Validation Entry Reaper Thread")
                .scheduleAtFixedRate(jtiValidationStore::deleteExpired, reaperCleanupPeriod, reaperCleanupPeriod, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        jtiEntryReaperThread.cancel(true);
    }

    @Override
    public void prepare() {
        // TODO move in a separated extension?
        signatureSuiteRegistry.register(JSON_2020_SIGNATURE_SUITE, new Jws2020SignatureSuite(typeManager.getMapper(JSON_LD)));
    }

    @Provider
    public IdentityService createIdentityService(ServiceExtensionContext context) {
        var credentialServiceUrlResolver = new DidCredentialServiceUrlResolver(didResolverRegistry);
        var validationAction = tokenValidationAction();

        var credentialValidationService = new VerifiableCredentialValidationServiceImpl(createPresentationVerifier(context),
                trustedIssuerRegistry, revocationServiceRegistry, clock, typeManager.getMapper());

        return new IdentityAndTrustService(secureTokenService, issuerId,
                getCredentialServiceClient(context), validationAction, credentialServiceUrlResolver, claimTokenFunction,
                credentialValidationService);
    }

    @Provider
    public CredentialServiceClient getCredentialServiceClient(ServiceExtensionContext context) {
        if (credentialServiceClient == null) {
            credentialServiceClient = new DefaultCredentialServiceClient(httpClient, Json.createBuilderFactory(Map.of()),
                    typeManager, JSON_LD, typeTransformerRegistry, jsonLd, context.getMonitor());
        }
        return credentialServiceClient;
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

            presentationVerifier = new MultiFormatPresentationVerifier(issuerId, jwtVerifier, ldpVerifier);
        }
        return presentationVerifier;
    }


    @NotNull
    private TokenValidationAction tokenValidationAction() {
        return (tokenRepresentation) -> {
            var rules = rulesRegistry.getRules(DCP_SELF_ISSUED_TOKEN_CONTEXT);
            return tokenValidationService.validate(tokenRepresentation, didPublicKeyResolver, rules);
        };
    }


}
