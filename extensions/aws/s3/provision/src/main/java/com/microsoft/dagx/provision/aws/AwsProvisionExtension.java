/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.provision.aws;

import com.microsoft.dagx.provision.aws.provider.ClientProvider;
import com.microsoft.dagx.provision.aws.provider.SdkClientProvider;
import com.microsoft.dagx.provision.aws.s3.*;
import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.StatusCheckerRegistry;
import net.jodah.failsafe.RetryPolicy;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Set;

/**
 * Provides data transfer {@link com.microsoft.dagx.spi.transfer.provision.Provisioner}s backed by AWS services.
 */
public class AwsProvisionExtension implements ServiceExtension {
    @DagxSetting
    private static final String AWS_ACCESS_KEY = "dagx.aws.access.key";

    @DagxSetting
    private static final String AWS_SECRET_KEY = "dagx.aws.secret.access.key";

    private Monitor monitor;
    private SdkClientProvider clientProvider;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var provisionManager = context.getService(ProvisionManager.class);

        // create an S3 client provider that is shared across provisioners
        clientProvider = SdkClientProvider.Builder.newInstance().credentialsProvider(createCredentialsProvider(context)).build();
        context.registerService(ClientProvider.class, clientProvider);

        //noinspection unchecked
        var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        var s3BucketProvisioner = new S3BucketProvisioner(clientProvider, 3600, monitor, retryPolicy);
        provisionManager.register(s3BucketProvisioner);

        // register the generator
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerClientGenerator(new S3ResourceDefinitionClientGenerator());

        var statusCheckerReg = context.getService(StatusCheckerRegistry.class);
        statusCheckerReg.register(S3BucketProvisionedResource.class, new S3StatusChecker(clientProvider, retryPolicy));

        registerTypes(context.getTypeManager());

        monitor.info("Initialized AWS Provision extension");
    }

    @Override
    public Set<String> requires() {
        return Set.of("dagx:retry-policy", "dagx:statuschecker");
    }

    @Override
    public Set<String> provides() {
        return Set.of("dagx:clientprovider");
    }

    @Override
    public void start() {
        monitor.info("Started AWS Provision extension");
    }

    @Override
    public void shutdown() {
        try {
            clientProvider.shutdown();
        } catch (Exception e) {
            monitor.info("Error closing S3 client provider", e);
        }
        monitor.info("Shutdown AWS Provision extension");
    }

    @NotNull
    private AwsCredentialsProvider createCredentialsProvider(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var accessKey = vault.resolveSecret(AWS_ACCESS_KEY);
        if (accessKey == null) {
            monitor.severe("AWS access key was not found in the vault");
            accessKey = "empty_access_key";
        }
        var secretKey = vault.resolveSecret(AWS_SECRET_KEY);
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


