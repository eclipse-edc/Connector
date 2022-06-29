/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.serializer;

import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import org.eclipse.dataspaceconnector.ids.core.policy.IdsConstraintImpl;
import org.eclipse.dataspaceconnector.ids.spi.domain.DefaultValues;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JsonldSerDesTest {

    @Test
    void serialize() throws IOException {
        var serDes = new JsonldSerDes(mock(Monitor.class));

        var msg = new DescriptionRequestMessageBuilder()
                ._issuerConnector_(URI.create("test"))
                ._modelVersion_("4.2.0")
                .build();

        var stringWithoutContext = serDes.getObjectMapper().writeValueAsString(msg);
        assertFalse(stringWithoutContext.contains("@context"));

        var jsonWithoutContext = serDes.serialize(msg);
        assertFalse(jsonWithoutContext.contains("@context"));

        serDes.setContext(DefaultValues.CONTEXT);
        serDes.setSubtypes(IdsConstraintImpl.class);
        var jsonWithContext = serDes.serialize(msg);
        assertTrue(jsonWithContext.contains("@context"));
    }
}
