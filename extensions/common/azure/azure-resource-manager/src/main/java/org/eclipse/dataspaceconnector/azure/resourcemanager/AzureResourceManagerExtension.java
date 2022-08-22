/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.resourcemanager;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.SettingResolver;

import java.util.Objects;

/**
 * Provides Azure Identity SDK and Azure Resource Manager SDK objects configured based on runtime settings.
 */
@Provides({AzureEnvironment.class, TokenCredential.class, AzureProfile.class, AzureResourceManager.class})
public class AzureResourceManagerExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Azure Resource Manager";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var tenantId = requiredSetting(context, "edc.azure.tenant.id");
        var subscriptionId = requiredSetting(context, "edc.azure.subscription.id");

        // Detect credential source based on runtime environment, e.g. Azure CLI, environment variables
        var credential = new DefaultAzureCredentialBuilder().build();

        var azure = AzureEnvironment.AZURE;
        var profile = new AzureProfile(tenantId, subscriptionId, azure);
        var resourceManager = AzureResourceManager
                .authenticate(credential, profile)
                .withSubscription(subscriptionId);

        context.registerService(AzureEnvironment.class, azure);
        context.registerService(TokenCredential.class, credential);
        context.registerService(AzureProfile.class, profile);
        context.registerService(AzureResourceManager.class, resourceManager);
    }

    private static String requiredSetting(SettingResolver context, String s) {
        return Objects.requireNonNull(context.getSetting(s, null), s);
    }
}
