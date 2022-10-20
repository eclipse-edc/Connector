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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Custom Jackson serializer for objects of type XMLGregorianCalendar.
 */
public class XmlGregorianCalendarSerializer extends StdSerializer<XMLGregorianCalendar> {

    public XmlGregorianCalendarSerializer() {
        super(XMLGregorianCalendar.class);
    }

    @Override
    public void serialize(XMLGregorianCalendar calendar, JsonGenerator generator, SerializerProvider provider) throws IOException {
        var sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setCalendar(calendar.toGregorianCalendar());
        var formatted = sdf.format(calendar.toGregorianCalendar().getTime());

        generator.writeStartObject();
        generator.writeStringField("@value", formatted);
        generator.writeStringField("@type", "http://www.w3.org/2001/XMLSchema#dateTimeStamp");
        generator.writeEndObject();
    }

}
