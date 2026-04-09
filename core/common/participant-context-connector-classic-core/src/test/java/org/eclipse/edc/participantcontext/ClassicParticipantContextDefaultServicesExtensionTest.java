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

package org.eclipse.edc.participantcontext;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.participantcontext.connector.ClassicParticipantContextDefaultServicesExtension;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
public class ClassicParticipantContextDefaultServicesExtensionTest {

    @Test
    void verifyParticipantContextSupplier(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.participant.id", "participantId"
        )));

        var extension = factory.constructInstance(ClassicParticipantContextDefaultServicesExtension.class);

        var supplier = extension.participantContextSupplier();
        assertThat(supplier.get().getContent()).extracting(ParticipantContext::getParticipantContextId).isEqualTo("participantId");
    }

    @Test
    void verifyParticipantContextSupplierWithConfiguredParticipantContextId(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.participant.id", "participantId",
                "edc.participant.context.id", "participantContextId"
        )));

        var extension = factory.constructInstance(ClassicParticipantContextDefaultServicesExtension.class);

        var supplier = extension.participantContextSupplier();
        assertThat(supplier.get().getContent()).extracting(ParticipantContext::getParticipantContextId).isEqualTo("participantContextId");
    }
}
