/*
 *  Copyright (c) 2023 Amazon Web Services
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amazon Web Services - initial implementation
 *
 */

package org.eclipse.edc.vault.aws;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultCertificateResolver;
import org.eclipse.edc.spi.security.VaultPrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import static org.eclipse.edc.util.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.edc.util.string.StringUtils.isNullOrEmpty;

@Provides({ Vault.class, PrivateKeyResolver.class, CertificateResolver.class })
@Extension(value = org.eclipse.edc.vault.aws.AwsSecretsManagerVaultExtension.NAME)
public class AwsSecretsManagerVaultExtension implements ServiceExtension {
    public static final String NAME = "AWS Secrets Manager Vault";

    @Setting
    private static final String VAULT_AWS_REGION = "edc.vault.aws.region";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var vaultRegion = getMandatorySetting(context, VAULT_AWS_REGION);

        var smClient = buildSmClient(vaultRegion);
        var vault = new AwsSecretsManagerVault(smClient, context.getMonitor());

        context.registerService(Vault.class, vault);
        context.registerService(PrivateKeyResolver.class, new VaultPrivateKeyResolver(vault));
        context.registerService(CertificateResolver.class, new VaultCertificateResolver(vault));
    }

    private SecretsManagerClient buildSmClient(String vaultRegion) {
        var builder = SecretsManagerClient.builder()
                .region(Region.of(vaultRegion));
        return builder.build();
    }

    private String getMandatorySetting(ServiceExtensionContext context, String setting) {
        var value = context.getSetting(setting, null);
        if (isNullOrEmpty(value)) {
            value = propOrEnv(setting, null);
            if (isNullOrEmpty(value)) {
                throw new AwsSecretsManagerVaultException(String.format("'%s' must be supplied but was null", setting));
            }
        }
        return value;
    }

}
