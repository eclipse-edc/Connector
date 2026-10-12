/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.validation;

import jakarta.json.Json;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.RetireAgreement.RETIRE_AGREEMENT_REASON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class RetireAgreementValidatorTest {

    private final Validator<jakarta.json.JsonObject> validator = RetireAgreementValidator.instance();

    @Test
    void shouldSucceed_whenReasonIsPresent() {
        var input = createObjectBuilder()
                .add(RETIRE_AGREEMENT_REASON, createArrayBuilder().add(createObjectBuilder().add(VALUE, "a reason")))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenReasonIsMissing() {
        var input = Json.createObjectBuilder().build();

        var result = validator.validate(input);

        assertThat(result).isFailed();
    }
}
