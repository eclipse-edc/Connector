/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.aws;

import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.transfer.provision.aws.provider.ClientProvider;
import com.microsoft.dagx.transfer.provision.aws.provider.SdkClientProvider;
import com.microsoft.dagx.transfer.provision.aws.s3.S3BucketProvisionedResource;
import com.microsoft.dagx.transfer.provision.aws.s3.S3BucketProvisioner;
import com.microsoft.dagx.transfer.provision.aws.s3.S3BucketResourceDefinition;
import com.microsoft.dagx.transfer.provision.aws.s3.S3ResourceDefinitionClientGenerator;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Set;

/**
 * Provides data transfer {@link com.microsoft.dagx.spi.transfer.provision.Provisioner}s backed by Azure services.
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

        var s3BucketProvisioner = new S3BucketProvisioner(clientProvider, 3600, monitor);
        provisionManager.register(s3BucketProvisioner);

        // register the generator
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerClientGenerator(new S3ResourceDefinitionClientGenerator());

        registerTypes(context.getTypeManager());

        //todo: remove this!!!
        context.registerService(ClientProvider.class, clientProvider);

        monitor.info("Initialized AWS Provision extension");
    }

    @Override
    public Set<String> provides() {
        return Set.of("client-provider");
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
        var credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return () -> credentials;
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(S3BucketProvisionedResource.class, S3BucketResourceDefinition.class);
    }


}


