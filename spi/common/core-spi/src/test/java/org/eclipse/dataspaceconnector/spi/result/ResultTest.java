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

package org.eclipse.dataspaceconnector.spi.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {


    @Test
    void verifyFailureMessages_whenSucceeded() {
        var r = Result.success("Foobar");
        assertThat(r.getFailureDetail()).isNull();
        assertThat(r.getFailureMessages()).isEmpty();
    }

    @Test
    void map_appliesFunctionToContent() {
        var result = Result.success("successful").map(it -> it + " and mapped");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo("successful and mapped");
    }

    @Test
    void verifyContent_whenFailed() {
        var r = Result.failure("random failure");
        assertThat(r.getContent()).isNull();
    }

    @Test
    void map_doesNothingOnFailedResult() {
        var result = Result.failure("error").map(it -> it + " and mapped");

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("error");
    }

    @Test
    void merge_twoFailures() {
        var r1 = Result.failure("reason 1");
        var r2 = Result.failure("reason 2");

        var result = r1.merge(r2);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).hasSize(2)
                .containsExactly("reason 1", "reason 2");
    }

    @Test
    void merge_failureAndSuccess() {
        // cannot use var, otherwise we'd have a Result<Object> and a Result<String>
        Result<Object> r1 = Result.failure("reason 1");
        Result<Object> r2 = Result.success("success message");

        var result = r1.merge(r2);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .hasSize(1)
                .containsExactly("reason 1");

    }

    @Test
    void merge_twoSuccesses() {
        var r1 = Result.success("msg 1");
        var r2 = Result.success("msg 2");

        var result = r1.merge(r2);
        assertThat(result.succeeded()).isTrue();
    }
}