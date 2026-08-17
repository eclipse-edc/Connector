/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.oauth2.tokenexchange.logic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileWorkloadCredentialSourceTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldReadAndTrimTheCredential() throws IOException {
        var file = Files.writeString(tempDir.resolve("token"), "  a-workload-credential\n");

        var result = new FileWorkloadCredentialSource(file.toString()).get();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo("a-workload-credential");
    }

    @Test
    void shouldReReadTheFileOnEveryInvocation() throws IOException {
        var file = tempDir.resolve("token");
        Files.writeString(file, "first");
        var source = new FileWorkloadCredentialSource(file.toString());

        assertThat(source.get().getContent()).isEqualTo("first");

        Files.writeString(file, "rotated");

        assertThat(source.get().getContent()).isEqualTo("rotated");
    }

    @Test
    void shouldFail_whenTheFileDoesNotExist() {
        var result = new FileWorkloadCredentialSource(tempDir.resolve("missing").toString()).get();

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("missing");
    }
}
