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

package org.eclipse.edc.protocol.ids.jsonld.type.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import org.eclipse.edc.protocol.ids.jsonld.JsonLd;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;

class XmlGregorianCalendarModuleTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var typeManager = new TypeManager();
        var customMapper = JsonLd.getObjectMapper();
        typeManager.registerContext("ids", customMapper);

        objectMapper = typeManager.getMapper("ids");
    }

    @Test
    void serializeAndDeserializeDate() throws IOException {
        var xmlGregorian = CalendarUtil.gregorianNow();

        var serialized = objectMapper.writeValueAsString(xmlGregorian);
        var xmlGregorianDeserialized = objectMapper.readValue(serialized, XMLGregorianCalendar.class);
        var serialized2 = objectMapper.writeValueAsString(xmlGregorianDeserialized);

        assertThat(xmlGregorian).isEqualTo(xmlGregorianDeserialized);
        assertThat(serialized).isEqualTo(serialized2);
    }

    @Test
    void serializeAndDeserializeAgreement() throws IOException {
        var xmlGregorian = CalendarUtil.gregorianNow();
        var agreement = new ContractAgreementBuilder()
                ._contractDate_(xmlGregorian)
                ._contractStart_(xmlGregorian)
                ._contractEnd_(xmlGregorian)
                .build();

        var serialized = objectMapper.writeValueAsString(agreement);
        var agreementDeserialized = objectMapper.readValue(serialized, ContractAgreement.class);
        var serialized2 = objectMapper.writeValueAsString(agreementDeserialized);

        assertThat(agreement).isEqualTo(agreementDeserialized);
        assertThat(serialized).isEqualTo(serialized2);
    }

}
