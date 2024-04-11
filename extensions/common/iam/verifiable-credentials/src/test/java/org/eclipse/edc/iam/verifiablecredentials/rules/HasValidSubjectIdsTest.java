/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.rules;


import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class HasValidSubjectIdsTest {
    private static final String SUBJECT_ID = "did:web:holder";

    @Test
    void singleSubject_matches() {

        var vc = createCredentialBuilder()
                .credentialSubjects(List.of(createSubject(SUBJECT_ID)))
                .build();
        assertThat(new HasValidSubjectIds(SUBJECT_ID).apply(vc)).isSucceeded();
    }

    @Test
    void singleSubject_doesNotMatch() {
        var vc = createCredentialBuilder()
                .credentialSubjects(List.of(createSubject("violating-id")))
                .build();
        assertThat(new HasValidSubjectIds(SUBJECT_ID).apply(vc)).isFailed()
                .detail().isEqualTo("Not all credential subject IDs match the expected subject ID '%s'. Violating subject IDs: [violating-id]".formatted(SUBJECT_ID));
    }

    @Test
    void multipleSubjects_allMatch() {
        var vc = createCredentialBuilder()
                .credentialSubjects(List.of(createSubject(SUBJECT_ID), createSubject(SUBJECT_ID)))
                .build();
        assertThat(new HasValidSubjectIds(SUBJECT_ID).apply(vc)).isSucceeded();
    }

    @Test
    void multipleSubjects_singleMismatch() {
        var vc = createCredentialBuilder()
                .credentialSubjects(List.of(createSubject(SUBJECT_ID), createSubject("violating-id")))
                .build();
        assertThat(new HasValidSubjectIds(SUBJECT_ID).apply(vc)).isFailed()
                .detail().isEqualTo("Not all credential subject IDs match the expected subject ID '%s'. Violating subject IDs: [violating-id]".formatted(SUBJECT_ID));
    }

    private CredentialSubject createSubject(String id) {
        return CredentialSubject.Builder.newInstance()
                .claim("test-claim", "test-value")
                .id(id)
                .build();
    }
}