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

import org.eclipse.edc.iam.decentralizedclaims.core.defaults.DefaultDcpParticipantIdExtractionFunction;
import org.eclipse.edc.iam.decentralizedclaims.core.defaults.DefaultTrustedIssuerRegistry;
import org.eclipse.edc.iam.decentralizedclaims.core.defaults.InMemorySignatureSuiteRegistry;
import org.eclipse.edc.iam.decentralizedclaims.core.scope.DcpScopeExtractorRegistry;
import org.eclipse.edc.iam.decentralizedclaims.core.scope.defaults.InMemoryDcpScopeStore;
import org.eclipse.edc.iam.decentralizedclaims.spi.ClaimTokenCreatorFunction;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.Map;

import static org.eclipse.edc.spi.result.Result.success;

@Extension("DCP Extension to register default services")
public class DcpDefaultServicesExtension implements ServiceExtension {

    public static final String CLAIMTOKEN_VC_KEY = "vc";

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Provider(isDefault = true)
    public TrustedIssuerRegistry createInMemoryIssuerRegistry() {
        return new DefaultTrustedIssuerRegistry();
    }

    @Provider(isDefault = true)
    public SignatureSuiteRegistry createSignatureSuiteRegistry() {
        return new InMemorySignatureSuiteRegistry();
    }

    @Provider(isDefault = true)
    public DefaultParticipantIdExtractionFunction defaultParticipantIdExtractionFunction() {
        return new DefaultDcpParticipantIdExtractionFunction();
    }

    @Provider(isDefault = true)
    public ScopeExtractorRegistry scopeExtractorRegistry() {
        return new DcpScopeExtractorRegistry();
    }

    // Default audience for DCP is the counter-party id
    @Provider(isDefault = true)
    public AudienceResolver defaultAudienceResolver() {
        return (msg) -> Result.success(msg.getCounterPartyId());
    }

    // Default ClaimToken creator function, will use "vc" as key
    @Provider(isDefault = true)
    public ClaimTokenCreatorFunction defaultClaimTokenFunction() {
        return credentials -> {
            var b = ClaimToken.Builder.newInstance()
                    .claims(Map.of(CLAIMTOKEN_VC_KEY, credentials));
            return success(b.build());
        };
    }
    
    @Provider(isDefault = true)
    public DcpScopeStore scopeStore() {
        return new InMemoryDcpScopeStore(criterionOperatorRegistry);
    }
}
