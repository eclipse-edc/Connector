package org.eclipse.dataspaceconnector.spi.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void map_appliesFunctionToContent() {
        var result = Result.success("successful").map(it -> it + " and mapped");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo("successful and mapped");
    }

    @Test
    void map_doesNothingOnFailedResult() {
        var result = Result.failure("error").map(it -> it + " and mapped");

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("error");
    }

}