package org.eclipse.dataspaceconnector.common.validator;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidValidatorTest {

    UuidValidator uuidValidator = new UuidValidator();

    @Test
    void isValid_valid_UUID() {
        String uuid = UUID.randomUUID().toString();
        assertThat(uuidValidator.isValid(uuid, null)).isTrue();
    }

    @Test
    void isValid_invalid_UUID() {
        String uuid = "invalid-uuid-string";
        assertThat(uuidValidator.isValid(uuid, null)).isFalse();
    }
}
