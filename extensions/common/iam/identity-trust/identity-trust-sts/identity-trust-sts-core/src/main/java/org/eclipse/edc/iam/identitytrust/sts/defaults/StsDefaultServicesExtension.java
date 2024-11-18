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

import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsAccountServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

@Extension(StsDefaultServicesExtension.NAME)
public class StsDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service Default Services";

    @Setting(description = "Self-issued ID Token expiration in minutes. By default is 5 minutes", defaultValue = "" + StsDefaultServicesExtension.DEFAULT_STS_TOKEN_EXPIRATION_MIN, key = "edc.iam.sts.token.expiration")
    private int tokenExpirationMinutes;

    private static final int DEFAULT_STS_TOKEN_EXPIRATION_MIN = 5;

    @Inject
    private StsAccountStore clientStore;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private Vault vault;

    @Inject
    private JwsSignerProvider jwsSignerProvider;

    @Inject
    private Clock clock;
    @Inject(required = false)
    private StsClientSecretGenerator stsClientSecretGenerator;

    @Inject
    private JtiValidationStore jtiValidationStore;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsClientTokenGeneratorService clientTokenService(ServiceExtensionContext context) {
        return new StsClientTokenGeneratorServiceImpl(
                (client) -> new JwtGenerationService(jwsSignerProvider),
                StsAccount::getPrivateKeyAlias,
                clock,
                TimeUnit.MINUTES.toSeconds(tokenExpirationMinutes),
                jtiValidationStore);
    }

    @Provider
    public StsAccountService clientService() {
        return new StsAccountServiceImpl(clientStore, vault, transactionContext, stsClientSecretGenerator());
    }


    private StsClientSecretGenerator stsClientSecretGenerator() {
        return ofNullable(stsClientSecretGenerator)
                .orElseGet(RandomStringGenerator::new);
    }
}
