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

package org.eclipse.dataspaceconnector.serializer.jsonld;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsConstants;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JsonldSerializerTest {

    @Test
    void toRdf() throws JsonProcessingException {
        var serializer = new JsonldSerializer(mock(Monitor.class));

        var msg = new DescriptionRequestMessageBuilder()
                ._issuerConnector_(URI.create("test"))
                ._modelVersion_("4.2.0")
                .build();

        var stringWithoutContext = serializer.getObjectMapper().writeValueAsString(msg);
        assertFalse(stringWithoutContext.contains("@context"));

        var jsonWithoutContext = serializer.toRdf(msg);
        assertFalse(jsonWithoutContext.contains("@context"));

        serializer.setContext(IdsConstants.IDS_CONTEXT_INFORMATION);
        var jsonWithContext = serializer.toRdf(msg);
        assertTrue(jsonWithContext.contains("@context"));
        assertTrue(jsonWithContext.startsWith("{\"@context\": "));
    }
}
