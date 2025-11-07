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
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core;

import jakarta.json.Json;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.core.defaults.DefaultCredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.core.validation.SelfIssueIdTokenValidationAction;
import org.eclipse.edc.iam.decentralizedclaims.service.DcpIdentityService;
import org.eclipse.edc.iam.decentralizedclaims.service.DidCredentialServiceUrlResolver;
import org.eclipse.edc.iam.decentralizedclaims.service.verification.MultiFormatPresentationVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.ClaimTokenCreatorFunction;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.DcpParticipantAgentServiceExtension;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.validation.TokenValidationAction;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JsonObjectToPresentationResponseMessageTransformer;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.VerifiableCredentialValidationServiceImpl;
import org.eclipse.edc.iam.verifiablecredentials.revocation.bitstring.BitstringStatusListRevocationService;
import org.eclipse.edc.iam.verifiablecredentials.revocation.statuslist2021.StatusList2021RevocationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Status;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
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
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.jwt.rules.HasSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.JtiValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.SubJwkIsNullRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.TokenNotNullRule;
import org.eclipse.edc.verifiablecredentials.linkeddata.DidMethodResolver;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DCP_CONTEXT_URL;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_0_8;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.STATUSLIST_2021_URL;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier.JWT_VC_TOKEN_CONTEXT;

@Extension("DCP Core Extension")
public class DcpCoreExtension implements ServiceExtension {

    public static final long DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS = 15 * 60 * 1000L;
    public static final String DCP_SELF_ISSUED_TOKEN_CONTEXT = "dcp-si";
    public static final String DCP_CLIENT_CONTEXT = "dcp-client";
    public static final String JSON_2020_SIGNATURE_SUITE = "JsonWebSignature2020";
    public static final long DEFAULT_CLEANUP_PERIOD_SECONDS = 60;
    @Setting(description = "DID of the participant")
    private static final String ISSUER_ID_KEY = "edc.iam.issuer.id";
    @Setting(description = "Validity period of cached StatusList2021 credential entries in milliseconds.", defaultValue = DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS + "", key = "edc.iam.credential.revocation.cache.validity")
    private long revocationCacheValidity;
    @Setting(description = "The period of the JTI entry reaper thread in seconds", defaultValue = DEFAULT_CLEANUP_PERIOD_SECONDS + "", key = "edc.sql.store.jti.cleanup.period")
    private long reaperCleanupPeriod;

    @Setting(description = "If set enable the dcp v0.8 namespace will be used", key = "edc.dcp.v08.forced", required = false, defaultValue = "false")
    private boolean enableDcpV08;

    @Setting(description = "Activate or deactivate JTI validation", key = "edc.iam.accesstoken.jti.validation", defaultValue = "true")
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
    private PresentationVerifier presentationVerifier;
    private CredentialServiceClient credentialServiceClient;
    private ScheduledFuture<?> jtiEntryReaperThread;
    @Setting(key = "edc.iam.credential.revocation.mimetype", description = "A comma-separated list of accepted content types of the revocation list credential.", defaultValue = "*/*")
    private String contentTypes;

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

        // register revocation services
        var acceptedContentTypes = parseAcceptedContentTypes(contentTypes);
        revocationServiceRegistry.addService(StatusList2021Status.TYPE, new StatusList2021RevocationService(typeManager.getMapper(), revocationCacheValidity, acceptedContentTypes, httpClient));
        revocationServiceRegistry.addService(BitstringStatusListStatus.TYPE, new BitstringStatusListRevocationService(typeManager.getMapper(), revocationCacheValidity, acceptedContentTypes, httpClient));
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
        var credentialServiceUrlResolver = new DidCredentialServiceUrlResolver(didResolverRegistry);
        var validationAction = tokenValidationAction();

        var credentialValidationService = new VerifiableCredentialValidationServiceImpl(createPresentationVerifier(context),
                trustedIssuerRegistry, revocationServiceRegistry, clock, typeManager.getMapper());

        return new DcpIdentityService(secureTokenService, this::didResolver,
                getCredentialServiceClient(context), validationAction, credentialServiceUrlResolver, claimTokenFunction,
                credentialValidationService);
    }

    @Provider
    public CredentialServiceClient getCredentialServiceClient(ServiceExtensionContext context) {
        if (credentialServiceClient == null) {

            var clientTypeTransformerRegistry = typeTransformerRegistry.forContext(DCP_CLIENT_CONTEXT);
            clientTypeTransformerRegistry.register(new JsonObjectToPresentationResponseMessageTransformer(typeManager, JSON_LD, dcpNamespace()));


            credentialServiceClient = new DefaultCredentialServiceClient(httpClient, Json.createBuilderFactory(Map.of()),
                    typeManager, JSON_LD, clientTypeTransformerRegistry, jsonLd, context.getMonitor(), dcpContext(), !enableDcpV08);
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

            presentationVerifier = new MultiFormatPresentationVerifier(jwtVerifier, ldpVerifier);
        }
        return presentationVerifier;
    }

    private Collection<String> parseAcceptedContentTypes(String contentTypes) {
        return List.of(contentTypes.split(","));
    }

    private JsonLdNamespace dcpNamespace() {
        return enableDcpV08 ? DSPACE_DCP_NAMESPACE_V_0_8 : DSPACE_DCP_NAMESPACE_V_1_0;
    }

    private String dcpContext() {
        return enableDcpV08 ? DCP_CONTEXT_URL : DSPACE_DCP_V_1_0_CONTEXT;
    }

    private String didResolver(String participantContext) {
        return participantContextConfig.getString(participantContext, ISSUER_ID_KEY);
    }

    @NotNull
    private TokenValidationAction tokenValidationAction() {
        return new SelfIssueIdTokenValidationAction(tokenValidationService, rulesRegistry, didPublicKeyResolver, this::didResolver);
    }

}
