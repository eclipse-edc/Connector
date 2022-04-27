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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Custom Jackson deserializer for objects of type XMLGregorianCalendar. Serves as a workaround
 * for date deserialization until integration of the IDS Information Model Serializer.
 */
public class XmlGregorianCalendarDeserializer extends StdDeserializer<XMLGregorianCalendar> {
    
    public XmlGregorianCalendarDeserializer() {
        super(XMLGregorianCalendar.class);
    }
    
    @Override
    public XMLGregorianCalendar deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.parse(jsonParser.getValueAsString())));
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }
    
}
