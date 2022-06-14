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

package org.eclipse.dataspaceconnector.serializer.jsonld.calendar;

import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.serializer.jsonld.JsonldSerializer;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class XmlGregorianCalendarModuleTest {

    private JsonldSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JsonldSerializer(mock(Monitor.class));
    }

    @Test
    void serializeAndDeserializeDate() {
        var xmlGregorian = CalendarUtil.gregorianNow();

        try {
            var serialized = serializer.toRdf(xmlGregorian);
            var xmlGregorianDeserialized = serializer.getObjectMapper().readValue(serialized, XMLGregorianCalendar.class);

            assertThat(xmlGregorian).isEqualTo(xmlGregorianDeserialized);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void serializeAndDeserializeAgreement() {
        var xmlGregorian = CalendarUtil.gregorianNow();

        var agreement = new ContractAgreementBuilder()
                ._contractDate_(xmlGregorian)
                ._contractStart_(xmlGregorian)
                ._contractEnd_(xmlGregorian)
                .build();

        try {
            var serialized = serializer.toRdf(agreement);
            var agreementDeserialized = serializer.getObjectMapper().readValue(serialized, ContractAgreement.class);

            assertThat(agreement).isEqualTo(agreementDeserialized);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
