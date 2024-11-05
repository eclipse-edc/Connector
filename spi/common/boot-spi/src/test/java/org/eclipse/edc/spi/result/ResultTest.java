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

package org.eclipse.edc.spi.result;

import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
        assertThat(result.getFailureMessages()).hasSize(2).containsExactly("reason 1", "reason 2");
    }

    @Test
    void merge_failureAndSuccess() {
        // cannot use var, otherwise we'd have a Result<Object> and a Result<String>
        Result<Object> r1 = Result.failure("reason 1");
        Result<Object> r2 = Result.success("success message");

        var result = r1.merge(r2);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).hasSize(1).containsExactly("reason 1");

    }

    @Test
    void merge_twoSuccesses() {
        var r1 = Result.success("msg 1");
        var r2 = Result.success("msg 2");

        var result = r1.merge(r2);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void onSuccess_whenSucceeded() {
        var result = Result.success("foo");
        Consumer<String> consumer = mock();

        assertThat(result.onSuccess(consumer)).isEqualTo(result);
        verify(consumer).accept(eq("foo"));
    }

    @Test
    void onSuccess_whenFailed() {
        Consumer<String> consumer = mock();

        Result<String> result = Result.failure("bar");
        assertThat(result.onSuccess(consumer)).isEqualTo(result);
        verifyNoInteractions(consumer);
    }

    @Test
    void onFailure_whenSucceeded() {
        var result = Result.success("foo");
        Consumer<Failure> consumer = mock();

        assertThat(result.onFailure(consumer)).isEqualTo(result);
        verifyNoInteractions(consumer);
    }

    @Test
    void onFailure_whenFailed() {
        Consumer<Failure> consumer = mock();

        Result<String> result = Result.failure("bar");
        assertThat(result.onFailure(consumer)).isEqualTo(result);
        verify(consumer).accept(argThat(f -> f.getMessages().contains("bar")));
    }

    @Test
    void mapToEmpty_succeeded() {
        var result = Result.success("foobar");

        Result<Void> mapped = result.mapEmpty();

        assertThat(mapped).isSucceeded().isNull();
    }

    @Test
    void mapToEmpty_failed() {
        var result = Result.failure("foobar");

        Result<String> mapped = result.mapEmpty();

        assertThat(mapped).isFailed().detail().isEqualTo("foobar");
    }

    @Test
    void whenSuccess_chainsSuccesses() {
        var result1 = Result.success("res1");
        var finalResult = result1.flatMap(r -> Result.success("res2")).flatMap(r -> Result.success("res3"));

        assertThat(finalResult.succeeded()).isTrue();
        assertThat(finalResult.getContent()).isEqualTo("res3");
    }

    @Test
    void whenSuccess_middleOneFails() {
        var result1 = Result.success("res1");
        Result<String> finalResult = result1
                .flatMap(r -> Result.success("res2"))
                .flatMap(r -> Result.failure("some failure"))
                .flatMap(Result::mapEmpty);

        assertThat(finalResult.failed()).isTrue();
        assertThat(finalResult.getFailureDetail()).isEqualTo("some failure");
    }

    @Test
    void whenSuccess_firstOneFails() {
        Result<String> result1 = Result.failure("fail1");
        var finalResult = result1
                .flatMap(r -> Result.failure("fail2"))
                .flatMap(r -> Result.failure("fail3"));

        assertThat(finalResult.failed()).isTrue();
        assertThat(finalResult.getFailureDetail()).isEqualTo("fail3");
    }

    @Test
    void asOptional() {
        assertThat(Result.success("some value").asOptional()).hasValue("some value");
        assertThat(Result.success().asOptional()).isEmpty();
        assertThat(Result.failure("foobar").asOptional()).isEmpty();
    }

    @Test
    void from() {
        var res = Result.from(Optional.of("some val"));
        assertThat(res.succeeded()).isTrue();
        assertThat(res.getContent()).isEqualTo("some val");

        var failedRes = Result.from(Optional.empty());
        assertThat(failedRes.failed()).isTrue();
        assertThat(failedRes.getFailureDetail()).isNotNull();
    }

    @Test
    void orElseThrow() {
        assertThat(Result.success("foobar").orElseThrow(failure -> new RuntimeException()))
                .isEqualTo("foobar");

        assertThatThrownBy(() -> Result.failure("barbaz").orElseThrow(failure -> new RuntimeException()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void compose_applyMappingFunctionIfSuccessful() {
        Function<String, Result<String>> mappingFunction = string -> string.equals("right content")
                ? Result.success("the content was good")
                : Result.failure("the content was not good");

        assertThat(Result.success("right content").compose(mappingFunction)).isSucceeded()
                .isEqualTo("the content was good");

        assertThat(Result.success("not right content").compose(mappingFunction)).isFailed()
                .extracting(Failure::getMessages).asList().contains("the content was not good");
    }

    @Test
    void compose_doNothingIfFailed() {
        var result = Result.failure("error").compose(it -> Result.success());

        assertThat(result).isFailed().extracting(Failure::getMessages).asList().contains("error");
    }

    @Test
    void recover_shouldApplyMappingFunction_whenFailed() {
        var failedResult = Result.<String>failure("failure");
        Function<Failure, Result<String>> successfulRecoverFunction = f -> Result.success("ok");
        Function<Failure, Result<String>> failingRecoverFunction = f -> Result.failure("error");

        assertThat(failedResult.recover(successfulRecoverFunction)).isSucceeded().isEqualTo("ok");
        assertThat(failedResult.recover(failingRecoverFunction)).isFailed().extracting(Failure::getMessages).asList().contains("error");
    }

    @Test
    void recover_shouldDoNothing_whenSucceeded() {
        var succeededResult = Result.success("ok");
        Function<Failure, Result<String>> failingRecoverFunction = f -> Result.failure("error");

        assertThat(succeededResult.recover(failingRecoverFunction)).isSucceeded();
    }

    @Test
    void ofThrowable_success() {

        var result = Result.ofThrowable(String::new);
        assertThat(result).isSucceeded().isEqualTo("");
    }

    @Test
    void ofThrowable_failure() {

        var result = Result.ofThrowable(() -> {
            throw new EdcException("Exception");
        });
        assertThat(result).isFailed().detail().contains("Exception");
    }
}
