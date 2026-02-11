/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.revocation.RevocationServiceRegistryImpl;
import org.eclipse.edc.iam.verifiablecredentials.revocation.bitstring.BitstringStatusListRevocationService;
import org.eclipse.edc.iam.verifiablecredentials.revocation.statuslist2021.StatusList2021RevocationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Status;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.util.List;

import static org.eclipse.edc.iam.verifiablecredentials.RevocationServiceRegistryExtension.NAME;
import static org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry.WILDCARD;

@Extension(value = NAME)
public class RevocationServiceRegistryExtension implements ServiceExtension {

    public static final String NAME = "Revocation Service Registry";
    public static final long DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS = 15 * 60 * 1000L;

    @Setting(
            key = "edc.iam.credential.revocation.cache.validity",
            description = "Validity period of cached StatusList2021 credential entries in milliseconds.",
            defaultValue = DEFAULT_REVOCATION_CACHE_VALIDITY_MILLIS + "")
    private long revocationCacheValidity;
    @Setting(
            key = "edc.iam.credential.revocation.mimetype",
            description = "A comma-separated list of accepted content types of the revocation list credential.",
            defaultValue = WILDCARD)
    private String contentTypes;

    @Inject
    private TypeManager typeManager;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public RevocationServiceRegistry createRevocationListService(ServiceExtensionContext context) {
        var revocationServiceRegistry = new RevocationServiceRegistryImpl(context.getMonitor());
        var acceptedContentTypes = List.of(contentTypes.split(","));
        revocationServiceRegistry.addService(StatusList2021Status.TYPE, new StatusList2021RevocationService(typeManager.getMapper(),
                revocationCacheValidity, acceptedContentTypes, httpClient, tokenValidationService, didPublicKeyResolver));
        revocationServiceRegistry.addService(BitstringStatusListStatus.TYPE, new BitstringStatusListRevocationService(typeManager.getMapper(),
                revocationCacheValidity, acceptedContentTypes, httpClient, tokenValidationService, didPublicKeyResolver));
        return revocationServiceRegistry;
    }
}
