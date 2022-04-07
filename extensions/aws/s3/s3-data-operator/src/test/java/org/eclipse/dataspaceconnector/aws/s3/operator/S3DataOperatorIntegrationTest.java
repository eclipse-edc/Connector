/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.s3.operator;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProvider;
import org.eclipse.dataspaceconnector.aws.testfixtures.AbstractS3Test;
import org.eclipse.dataspaceconnector.aws.testfixtures.TestS3ClientProvider;
import org.eclipse.dataspaceconnector.aws.testfixtures.annotations.AwsS3IntegrationTest;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AwsS3IntegrationTest
public class S3DataOperatorIntegrationTest extends AbstractS3Test {

    private final TypeManager typeManager = new TypeManager();
    private final S3ClientProvider clientProvider = new TestS3ClientProvider(getCredentials(), S3_ENDPOINT);
    private final Monitor monitor = new ConsoleMonitor();
    private final Vault vault = mock(Vault.class);

    @Test
    void shouldWriteAndReadFromS3Bucket() {
        var reader = new S3BucketReader(monitor, vault, clientProvider);
        var writer = new S3BucketWriter(monitor, typeManager, new RetryPolicy<>(), clientProvider);
        var address = DataAddress.Builder.newInstance()
                .type(TYPE)
                .keyName("key")
                .property(S3BucketSchema.REGION, "any")
                .property(S3BucketSchema.BUCKET_NAME, bucketName)
                .build();
        var credentials = getCredentials();
        var secret = typeManager.writeValueAsString(new AwsTemporarySecretToken(credentials.accessKeyId(), credentials.secretAccessKey(), "", 3600));
        when(vault.resolveSecret("aws-credentials")).thenReturn(secret);
        when(vault.resolveSecret("aws-access-key-id")).thenReturn(credentials.accessKeyId());
        when(vault.resolveSecret("aws-secret-access-key")).thenReturn(credentials.secretAccessKey());

        var writeResult = writer.write(address, "key", new ByteArrayInputStream("content".getBytes()), secret);
        assertThat(writeResult.succeeded()).isEqualTo(true);

        var result = reader.read(address);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasBinaryContent("content".getBytes());
    }

}
