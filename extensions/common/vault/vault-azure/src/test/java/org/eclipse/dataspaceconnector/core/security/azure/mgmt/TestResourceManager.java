/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.azure.mgmt;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.keyvault.KeyVaultManager;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestResourceManager {

    private final AzureResourceManager azureResourceManager;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String prefix = "dataspaceconnector-itest_";
    private final TokenCredential credential;
    private final AzureProfile profile;

    public TestResourceManager(String clientId, String tenantId, String clientSecret, String subsciptionId) {
        profile = new AzureProfile(tenantId, subsciptionId, AzureEnvironment.AZURE);
        credential = new ClientSecretCredentialBuilder()
                .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                .clientId(clientId)
                .tenantId(tenantId)
                .clientSecret(clientSecret)
                .build();

        azureResourceManager = AzureResourceManager
                .configure()
                .withLogLevel(HttpLogDetailLevel.BASIC)
                .authenticate(credential, profile)
                .withSubscription(subsciptionId);
    }

    public ResourceGroup createRandomResourceGroup() {

        var name = getRandomName();

        logger.info("Creating Resource Group \"" + name + "\"");

        return azureResourceManager.resourceGroups().define(name)
                .withRegion(Region.EUROPE_WEST)
                .create();
    }

    private String getRandomName() {
        return azureResourceManager.resourceGroups().manager().internalContext().randomResourceName(prefix, 24);
    }

    public void deleteResourceGroup(ResourceGroup rg) {
        logger.info("Deleting Resource Group \"" + rg.name() + "\"");
        azureResourceManager.resourceGroups().deleteByName(rg.name());
    }


    public Vault deployVault(ResourceGroup rg, String clientId) {
        final String vaultName = "dataspaceconnector-itest-vault";

        KeyVaultManager kvm = KeyVaultManager.authenticate(credential, profile);


        return kvm.vaults()
                .define(vaultName)
                .withRegion(Region.EUROPE_WEST)
                .withExistingResourceGroup(rg)
                .defineAccessPolicy()
                .forServicePrincipal(clientId)
                .allowSecretAllPermissions()
                .allowCertificateAllPermissions()
                .allowKeyAllPermissions()
                .attach()
                .create();
    }

    public void purgeVault(Vault vault) {
        azureResourceManager.vaults().purgeDeleted(vault.name(), "westeurope");
    }
}
