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

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.framework.iam.DefaultDataPlaneAccessTokenServiceImpl;
import org.eclipse.edc.connector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.edc.connector.dataplane.framework.registry.TransferServiceSelectionStrategy;
import org.eclipse.edc.connector.dataplane.framework.store.InMemoryAccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.framework.store.InMemoryDataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessControlService;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.LocalPublicKeyService;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.function.Supplier;

import static org.eclipse.edc.connector.dataplane.spi.TransferDataPlaneConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.connector.dataplane.spi.TransferDataPlaneConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;

@Extension(value = DataPlaneDefaultIamServicesExtension.NAME)
public class DataPlaneDefaultIamServicesExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Framework Default IAM Services";
    @Inject
    private AccessTokenDataStore accessTokenDataStore;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private PrivateKeyResolver privateKeyResolver;
    @Inject
    private LocalPublicKeyService localPublicKeyService;

    @Override
    public String name() {
        return NAME;
    }


    @Provider(isDefault = true)
    public DataPlaneAccessControlService defaultAccessControlService(ServiceExtensionContext context) {
        context.getMonitor().debug("DataPlane Access Control: default implementation is used, will always return Result.success()");
        return (claimToken, address, requestData) -> Result.success();
    }

    @Provider(isDefault = true)
    public DataPlaneAccessTokenService defaultAccessTokenService(ServiceExtensionContext context) {
        return new DefaultDataPlaneAccessTokenServiceImpl(new JwtGenerationService(), accessTokenDataStore, context.getMonitor().withPrefix("DataPlane IAM"), getPrivateKeySupplier(context), tokenValidationService, localPublicKeyService);
    }

    @NotNull
    private Supplier<PrivateKey> getPrivateKeySupplier(ServiceExtensionContext context) {
        return () -> {
            var alias = context.getConfig().getString(TOKEN_SIGNER_PRIVATE_KEY_ALIAS);
            return privateKeyResolver.resolvePrivateKey(alias)
                    .orElse(f -> {
                        context.getMonitor().warning("Cannot resolve private key: " + f.getFailureDetail());
                        return null;
                    });
        };
    }
}
