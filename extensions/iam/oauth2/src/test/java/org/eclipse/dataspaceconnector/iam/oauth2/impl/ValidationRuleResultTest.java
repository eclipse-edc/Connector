package org.eclipse.dataspaceconnector.iam.oauth2.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationRuleResultTest {

    @Test
    void isSuccess() {
        ValidationRuleResult result = new ValidationRuleResult();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void reportsError() {
        ValidationRuleResult result = new ValidationRuleResult();

        result.reportsError("This is an error");

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void getErrorMessages() {
        ValidationRuleResult result = new ValidationRuleResult();

        result.reportsError("This is an error");

        assertThat(result.getErrorMessages()).hasSize(1)
                .contains("This is an error");
    }


    @Test
    void reportsErrors() {
        ValidationRuleResult result = new ValidationRuleResult();

        result.reportsError("This is an error");
        result.reportsError("This is another error");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessages()).hasSize(2)
                .contains("This is an error")
                .contains("This is another error");
    }

    @Test
    void merge() {
        ValidationRuleResult result = new ValidationRuleResult();
        ValidationRuleResult other = new ValidationRuleResult();

        result.reportsError("This is an error");
        other.reportsError("This is another error");

        result.merge(other);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessages()).hasSize(2)
                .contains("This is an error")
                .contains("This is another error");
    }
}