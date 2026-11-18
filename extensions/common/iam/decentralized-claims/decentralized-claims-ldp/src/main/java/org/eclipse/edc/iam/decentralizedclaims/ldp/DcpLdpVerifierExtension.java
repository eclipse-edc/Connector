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

package org.eclipse.edc.iam.decentralizedclaims.ldp;

import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifierRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.verifiablecredentials.linkeddata.DidMethodResolver;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;

import java.time.Clock;

import static org.eclipse.edc.iam.decentralizedclaims.ldp.DcpLdpVerifierExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Adds the verification of Linked-Data (LDP) presentations and credentials to DCP.
 * <p>
 * LDP credentials are not supported by default, only JWT credentials are: runtimes that need to accept LDP
 * presentations have to add this extension explicitly.
 */
@Extension(NAME)
public class DcpLdpVerifierExtension implements ServiceExtension {

    public static final String NAME = "DCP Linked-Data Verifier Extension";
    public static final String JSON_2020_SIGNATURE_SUITE = "JsonWebSignature2020";

    @Setting(
            key = "edc.iam.presentation.binding.required",
            description = "Rejects Linked-Data presentations without a proof 'domain' and 'expires'. If disabled, these are only checked when present.",
            defaultValue = "false")
    private boolean requirePresentationBinding;

    @Inject
    private CredentialVerifierRegistry credentialVerifierRegistry;
    @Inject
    private SignatureSuiteRegistry signatureSuiteRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private DidResolverRegistry didResolverRegistry;
    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        signatureSuiteRegistry.register(JSON_2020_SIGNATURE_SUITE, new Jws2020SignatureSuite(typeManager.getMapper(JSON_LD)));

        var ldpVerifier = LdpVerifier.Builder.newInstance()
                .signatureSuites(signatureSuiteRegistry)
                .jsonLd(jsonLd)
                .typeManager(typeManager)
                .typeContext(JSON_LD)
                .methodResolver(new DidMethodResolver(didResolverRegistry))
                .clock(clock)
                .requireBinding(requirePresentationBinding)
                .build();

        credentialVerifierRegistry.register(ldpVerifier);
    }
}
