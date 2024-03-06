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

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.test.e2e.participant.DataPlaneParticipant;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractDataPlaneTest {
    protected static final DataPlaneParticipant DATAPLANE = DataPlaneParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();
    @RegisterExtension
    protected static EdcRuntimeExtension runtime =
            new EdcRuntimeExtension(
                    ":system-tests:e2e-dataplane-tests:runtimes:data-plane",
                    "data-plane",
                    DATAPLANE.dataPlaneConfiguration()
            );
}
