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

import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.IdentityAndTrustService;
import org.eclipse.edc.iam.identitytrust.validation.SelfIssuedIdTokenValidator;
import org.eclipse.edc.iam.identitytrust.verification.MultiFormatPresentationVerifier;
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
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.eclipse.edc.verification.jwt.SelfIssuedIdTokenVerifier;

import java.time.Clock;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

@Extension("Identity And Trust Extension")
public class IdentityAndTrustExtension implements ServiceExtension {

    @Setting(value = "DID of this connector", required = true)
    public static final String ISSUER_DID_PROPERTY = "edc.iam.issuer.id";

    @Inject
    private SecureTokenService secureTokenService;

    @Inject
    private PresentationVerifier presentationVerifier;

    @Inject
    private CredentialServiceClient credentialServiceClient;

    @Inject
    private DidResolverRegistry resolverRegistry;

    @Inject
    private TrustedIssuerRegistry registry;

    @Inject
    private TypeManager typeManager;

    @Inject
    private SignatureSuiteRegistry signatureSuiteRegistry;

    @Inject
    private JsonLd jsonLd;

    private JwtValidator jwtValidator;
    private JwtVerifier jwtVerifier;
    @Inject
    private Clock clock;

    @Provider
    public IdentityService createIdentityService(ServiceExtensionContext context) {
        return new IdentityAndTrustService(secureTokenService, getIssuerDid(context), context.getParticipantId(), presentationVerifier,
                credentialServiceClient, getJwtValidator(), getJwtVerifier(), registry, clock);
    }

    @Provider
    public JwtValidator getJwtValidator() {
        if (jwtValidator == null) {
            jwtValidator = new SelfIssuedIdTokenValidator();
        }
        return jwtValidator;
    }

    @Provider
    public PresentationVerifier createPresentationVerifier(ServiceExtensionContext context) {
        var mapper = typeManager.getMapper(JSON_LD);

        var jwtVerifier = new JwtPresentationVerifier(getJwtVerifier(), mapper);
        var ldpVerifier = LdpVerifier.Builder.newInstance()
                .signatureSuites(signatureSuiteRegistry)
                .jsonLd(jsonLd)
                .objectMapper(mapper)
                .build();

        return new MultiFormatPresentationVerifier(getOwnDid(context), jwtVerifier, ldpVerifier);
    }

    @Provider
    public JwtVerifier getJwtVerifier() {
        if (jwtVerifier == null) {
            jwtVerifier = new SelfIssuedIdTokenVerifier(resolverRegistry);
        }
        return jwtVerifier;
    }

    private String getOwnDid(ServiceExtensionContext context) {
        // todo: this must be config value
        return null;
    }

    private String getIssuerDid(ServiceExtensionContext context) {
        return context.getConfig().getString(ISSUER_DID_PROPERTY);
    }
}
