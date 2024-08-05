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

package org.eclipse.edc.iam.identitytrust.sts.defaults;

import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsClientServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.JwsSignerProvider;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

@Extension(StsDefaultServicesExtension.NAME)
public class StsDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service Default Services";

    @Setting(value = "Self-issued ID Token expiration in minutes. By default is 5 minutes", defaultValue = "" + StsDefaultServicesExtension.DEFAULT_STS_TOKEN_EXPIRATION_MIN)
    private static final String STS_TOKEN_EXPIRATION = "edc.iam.sts.token.expiration"; // in minutes

    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;

    @Inject
    private StsClientStore clientStore;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private Vault vault;

    @Inject
    private JwsSignerProvider jwsSignerProvider;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsClientTokenGeneratorService clientTokenService(ServiceExtensionContext context) {
        var tokenExpiration = context.getSetting(STS_TOKEN_EXPIRATION, DEFAULT_STS_TOKEN_EXPIRATION_MIN);
        return new StsClientTokenGeneratorServiceImpl(
                (client) -> new JwtGenerationService(jwsSignerProvider),
                StsClient::getPrivateKeyAlias,
                clock,
                TimeUnit.MINUTES.toSeconds(tokenExpiration));
    }

    @Provider
    public StsClientService clientService() {
        return new StsClientServiceImpl(clientStore, vault, transactionContext);
    }
}
