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
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.DidCredentialServiceUrlResolver;
import org.eclipse.edc.iam.identitytrust.IdentityAndTrustService;
import org.eclipse.edc.iam.identitytrust.core.defaults.DefaultCredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.validation.SelfIssuedIdTokenValidator;
import org.eclipse.edc.iam.identitytrust.verification.MultiFormatPresentationVerifier;
import org.eclipse.edc.identitytrust.AudienceResolver;
import org.eclipse.edc.identitytrust.CredentialServiceClient;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.identitytrust.TrustedIssuerRegistry;
import org.eclipse.edc.identitytrust.validation.JwtValidator;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.identitytrust.verification.PresentationVerifier;
import org.eclipse.edc.identitytrust.verification.SignatureSuiteRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.linkeddata.DidMethodResolver;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.eclipse.edc.verification.jwt.SelfIssuedIdTokenVerifier;

import java.time.Clock;
import java.util.Map;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Extension("Identity And Trust Extension")
public class IdentityAndTrustExtension implements ServiceExtension {

    @Setting(value = "DID of this connector", required = true)
    public static final String CONNECTOR_DID_PROPERTY = "edc.iam.issuer.id";


    @Inject
    private SecureTokenService secureTokenService;

    @Inject
    private TrustedIssuerRegistry registry;

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
    private PublicKeyResolver didPublicKeyResolver;

    @Inject
    private DidResolverRegistry didResolverRegistry;

    private JwtValidator jwtValidator;
    private JwtVerifier jwtVerifier;
    private PresentationVerifier presentationVerifier;
    private CredentialServiceClient credentialServiceClient;
    @Inject
    private AudienceResolver audienceResolver;

    @Provider
    public IdentityService createIdentityService(ServiceExtensionContext context) {
        var credentialServiceUrlResolver = new DidCredentialServiceUrlResolver(didResolverRegistry);
        return new IdentityAndTrustService(secureTokenService, getOwnDid(context), context.getParticipantId(), getPresentationVerifier(context),
                getCredentialServiceClient(context), getJwtValidator(), getJwtVerifier(), registry, clock, credentialServiceUrlResolver, audienceResolver);
    }

    @Provider
    public JwtValidator getJwtValidator() {
        if (jwtValidator == null) {
            jwtValidator = new SelfIssuedIdTokenValidator();
        }
        return jwtValidator;
    }

    @Provider
    public CredentialServiceClient getCredentialServiceClient(ServiceExtensionContext context) {
        if (credentialServiceClient == null) {
            credentialServiceClient = new DefaultCredentialServiceClient(httpClient, Json.createBuilderFactory(Map.of()),
                    typeManager.getMapper(JSON_LD), typeTransformerRegistry, jsonLd, context.getMonitor());
        }
        return credentialServiceClient;
    }

    @Provider
    public PresentationVerifier getPresentationVerifier(ServiceExtensionContext context) {
        if (presentationVerifier == null) {
            var mapper = typeManager.getMapper(JSON_LD);

            var jwtVerifier = new JwtPresentationVerifier(getJwtVerifier(), mapper);
            var ldpVerifier = LdpVerifier.Builder.newInstance()
                    .signatureSuites(signatureSuiteRegistry)
                    .jsonLd(jsonLd)
                    .objectMapper(mapper)
                    .methodResolver(new DidMethodResolver(didResolverRegistry))
                    .build();

            presentationVerifier = new MultiFormatPresentationVerifier(getOwnDid(context), jwtVerifier, ldpVerifier);
        }
        return presentationVerifier;
    }

    @Provider
    public JwtVerifier getJwtVerifier() {
        if (jwtVerifier == null) {
            jwtVerifier = new SelfIssuedIdTokenVerifier(didPublicKeyResolver);
        }
        return jwtVerifier;
    }


    private String getOwnDid(ServiceExtensionContext context) {
        return context.getConfig().getString(CONNECTOR_DID_PROPERTY);
    }
}
