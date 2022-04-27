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

package org.eclipse.dataspaceconnector.ids.core.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;

class XmlGregorianCalendarModuleTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        objectMapper.registerModule(new XmlGregorianCalendarModule());
    }
    
    @Test
    void serializeAndDeserializeDate() {
        var xmlGregorian = CalendarUtil.gregorianNow();
        
        try {
            var serialized = objectMapper.writeValueAsString(xmlGregorian);
            var xmlGregorianDeserialized = objectMapper.readValue(serialized, XMLGregorianCalendar.class);
            
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
            var serialized = objectMapper.writeValueAsString(agreement);
            var agreementDeserialized = objectMapper.readValue(serialized, ContractAgreement.class);
        
            assertThat(agreement).isEqualTo(agreementDeserialized);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
