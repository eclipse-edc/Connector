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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.aws.dataplane.s3;

import org.eclipse.dataspaceconnector.aws.s3.core.AwsClientProvider;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsSecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.aws.dataplane.s3.TestFunctions.VALID_ACCESS_KEY_ID;
import static org.eclipse.dataspaceconnector.aws.dataplane.s3.TestFunctions.VALID_BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.dataplane.s3.TestFunctions.VALID_REGION;
import static org.eclipse.dataspaceconnector.aws.dataplane.s3.TestFunctions.VALID_SECRET_ACCESS_KEY;
import static org.eclipse.dataspaceconnector.aws.dataplane.s3.TestFunctions.s3DataAddressWithCredentials;
import static org.eclipse.dataspaceconnector.aws.dataplane.s3.TestFunctions.s3DataAddressWithoutCredentials;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.REGION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3DataSinkFactoryTest {

    private final AwsClientProvider clientProvider = mock(AwsClientProvider.class);
    private final Vault vault = mock(Vault.class);
    private final TypeManager typeManager = new TypeManager();
    private final S3DataSinkFactory factory = new S3DataSinkFactory(clientProvider, mock(ExecutorService.class), mock(Monitor.class), vault, typeManager);

    @Test
    void canHandle_returnsTrueWhenExpectedType() {
        var dataAddress = DataAddress.Builder.newInstance().type(S3BucketSchema.TYPE).build();

        var result = factory.canHandle(createRequest(dataAddress));

        assertThat(result).isTrue();
    }

    @Test
    void canHandle_returnsFalseWhenUnexpectedType() {
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();

        var result = factory.canHandle(createRequest(dataAddress));

        assertThat(result).isFalse();
    }

    @Test
    void validate_ShouldSucceedIfPropertiesAreValid() {
        var destination = s3DataAddressWithCredentials();
        var request = createRequest(destination);

        var result = factory.validate(request);

        assertThat(result.succeeded()).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidInputs.class)
    void validate_shouldFailIfMandatoryPropertiesAreMissing(String bucketName, String region, String accessKeyId, String secretAccessKey) {
        var destination = DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .property(S3BucketSchema.BUCKET_NAME, bucketName)
                .property(REGION, region)
                .property(S3BucketSchema.ACCESS_KEY_ID, accessKeyId)
                .property(S3BucketSchema.SECRET_ACCESS_KEY, secretAccessKey)
                .build();

        var request = createRequest(destination);

        var result = factory.validate(request);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void createSink_shouldGetTheTemporarySecretTokenFromTheVault() {
        var destination = s3DataAddressWithCredentials();
        var temporaryKey = new AwsTemporarySecretToken("temporaryId", "temporarySecret", "temporaryToken", 10);
        when(vault.resolveSecret(destination.getKeyName())).thenReturn(typeManager.writeValueAsString(temporaryKey));
        var request = createRequest(destination);

        var sink = factory.createSink(request);

        assertThat(sink).isNotNull().isInstanceOf(S3DataSink.class);
        verify(clientProvider).s3Client(eq(VALID_REGION), isA(AwsTemporarySecretToken.class));
    }

    @Test
    void createSink_shouldCreateDataSinkWithCredentialsInDataAddressIfTheresNoSecret() {
        when(vault.resolveSecret(any())).thenReturn(null);
        var destination = s3DataAddressWithCredentials();
        var request = createRequest(destination);

        var sink = factory.createSink(request);

        assertThat(sink).isNotNull().isInstanceOf(S3DataSink.class);
        verify(clientProvider).s3Client(VALID_REGION, new AwsSecretToken(VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY));
    }

    @Test
    void createSink_shouldLetTheProviderGetTheCredentialsAsFallback() {
        when(vault.resolveSecret(any())).thenReturn(null);
        var destination = s3DataAddressWithoutCredentials();
        var request = createRequest(destination);

        var sink = factory.createSink(request);

        assertThat(sink).isNotNull().isInstanceOf(S3DataSink.class);
        verify(clientProvider).s3Client(VALID_REGION);
    }

    @Test
    void createSink_shouldThrowExceptionIfValidationFails() {
        var destination = DataAddress.Builder.newInstance()
                .type(S3BucketSchema.TYPE)
                .build();

        var request = createRequest(destination);

        assertThatThrownBy(() -> factory.createSink(request)).isInstanceOf(EdcException.class);
    }

    private static class InvalidInputs implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(VALID_BUCKET_NAME, " ", VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY),
                    Arguments.of(" ", VALID_REGION, VALID_ACCESS_KEY_ID, VALID_SECRET_ACCESS_KEY)
            );
        }
    }

    private DataFlowRequest createRequest(DataAddress destination) {
        return DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(DataAddress.Builder.newInstance().type(S3BucketSchema.TYPE).build())
                .destinationDataAddress(destination)
                .build();
    }
}