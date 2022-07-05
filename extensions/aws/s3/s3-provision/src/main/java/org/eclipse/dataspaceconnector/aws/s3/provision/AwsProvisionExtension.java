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

package org.eclipse.dataspaceconnector.aws.s3.provision;

import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsSecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.ClientProvider;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.s3.core.SdkClientProvider;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * Provides data transfer {@link org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner}s backed by AWS services.
 */
@Provides(ClientProvider.class)
public class AwsProvisionExtension implements ServiceExtension {
    @EdcSetting
    private static final String AWS_ACCESS_KEY = "edc.aws.access.key";

    @EdcSetting
    private static final String AWS_SECRET_KEY = "edc.aws.secret.access.key";

    @EdcSetting
    private static final String PROVISION_MAX_RETRY = "edc.aws.provision.retry.retries.max";

    @EdcSetting
    private static final String PROVISION_MAX_ROLE_SESSION_DURATION = "edc.aws.provision.role.duration.session.max";

    @Inject
    private Vault vault;

    private Monitor monitor;
    private SdkClientProvider clientProvider;

    @Override
    public String name() {
        return "AWS Provision";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var provisionManager = context.getService(ProvisionManager.class);

        // create an S3 client provider that is shared across provisioners
        clientProvider = SdkClientProvider.Builder.newInstance().credentialsProvider(createCredentialsProvider(context)).build();
        context.registerService(ClientProvider.class, clientProvider);

        @SuppressWarnings("unchecked") var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);

        int maxRetries = context.getSetting(PROVISION_MAX_RETRY, 10);
        int roleMaxSessionDuration = context.getSetting(PROVISION_MAX_ROLE_SESSION_DURATION, 3600);
        var provisionerConfiguration = new S3BucketProvisionerConfiguration(maxRetries, roleMaxSessionDuration);
        var s3BucketProvisioner = new S3BucketProvisioner(clientProvider, monitor, retryPolicy, provisionerConfiguration);
        provisionManager.register(s3BucketProvisioner);

        // register the generator
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerGenerator(new S3ConsumerResourceDefinitionGenerator());

        var statusCheckerReg = context.getService(StatusCheckerRegistry.class);
        statusCheckerReg.register(S3BucketSchema.TYPE, new S3StatusChecker(clientProvider, retryPolicy));

        registerTypes(context.getTypeManager());
    }

    @Override
    public void shutdown() {
        try {
            clientProvider.shutdown();
        } catch (Exception e) {
            monitor.info("Error closing S3 client provider", e);
        }
    }

    @NotNull
    private AwsCredentialsProvider createCredentialsProvider(ServiceExtensionContext context) {
        var accessKey = vault.resolveSecret(AwsProvisionExtension.AWS_ACCESS_KEY);
        if (accessKey == null) {
            monitor.severe("AWS access key was not found in the vault");
            accessKey = "empty_access_key";
        }
        var secretKey = vault.resolveSecret(AwsProvisionExtension.AWS_SECRET_KEY);
        if (secretKey == null) {
            monitor.severe("AWS secret key was not found in the vault");
            secretKey = "empty_secret_key";
        }

        if (vault.resolveSecret("aws-credentials") == null) {
            vault.storeSecret("aws-credentials", context.getTypeManager().writeValueAsString(new AwsSecretToken(accessKey, secretKey)));
        }
        var credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return () -> credentials;
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(S3BucketProvisionedResource.class, S3BucketResourceDefinition.class, AwsTemporarySecretToken.class);
    }


}


