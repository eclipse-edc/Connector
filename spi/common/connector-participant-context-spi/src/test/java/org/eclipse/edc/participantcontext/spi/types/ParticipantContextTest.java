/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.spi.types;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantContextTest {

    @Test
    void verifyCreateTimestamp() {
        var context = ParticipantContext.Builder.newInstance()
                              .participantContextId("test-id")
                              .build();

        assertThat(context.getCreatedAt()).isNotZero().isLessThanOrEqualTo(Instant.now().toEpochMilli());

        var context2 = ParticipantContext.Builder.newInstance()
                               .participantContextId("test-id")
                               .createdAt(42)
                               .build();

        assertThat(context2.getCreatedAt()).isEqualTo(42);
    }

    @Test
    void verifyLastModifiedTimestamp() {
        var context = ParticipantContext.Builder.newInstance()
                              .participantContextId("test-id")
                              .build();

        assertThat(context.getLastModified()).isNotZero().isEqualTo(context.getCreatedAt());

        var context2 = ParticipantContext.Builder.newInstance()
                               .participantContextId("test-id")
                               .lastModified(42)
                               .build();

        assertThat(context2.getLastModified()).isEqualTo(42);
    }

    @Test
    void verifyState() {
        var context = ParticipantContext.Builder.newInstance()
                              .participantContextId("test-id")
                              .state(ParticipantContextState.CREATED);

        assertThat(context.build().getState()).isEqualTo(0);
        assertThat(context.state(ParticipantContextState.ACTIVATED).build().getState()).isEqualTo(1);
        assertThat(context.state(ParticipantContextState.DEACTIVATED).build().getState()).isEqualTo(2);

    }

}