/*
 *  Copyright (c) 2023 NTT DATA Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Masatake Iwasaki (NTT DATA) - Initial implementation
 *
 */

package org.eclipse.edc.aws.s3;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

import static org.assertj.core.api.Assertions.assertThat;

public class AwsClientProviderImplTest {
    @Test
    void testClientCaching() {
        var configuration = AwsClientProviderConfiguration.Builder.newInstance()
                .credentialsProvider(() -> AwsBasicCredentials.create("dummy", "dummy"))
                .build();
        var provider = new AwsClientProviderImpl(configuration);
        var client1 = provider.s3Client("us-east-1");
        var client2 = provider.s3Client("us-west-1");
        var client3 = provider.s3Client("us-west-1");
        assertThat(client3).isSameAs(client2).isNotSameAs(client1);

        var asyncClient1 = provider.s3AsyncClient("us-east-1");
        var asyncClient2 = provider.s3AsyncClient("us-west-1");
        var asyncClient3 = provider.s3AsyncClient("us-west-1");
        assertThat(client3).isSameAs(client2).isNotSameAs(client1);
    }
}
