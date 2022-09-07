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

package org.eclipse.dataspaceconnector.ids.jsonld.type.calendar;

import com.fasterxml.jackson.databind.module.SimpleModule;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Custom Jackson module for the ObjectMapper, that contains custom (de)serializers for objects of
 * type XMLGregorianCalendar.
 */
public class XmlGregorianCalendarModule extends SimpleModule {

    public XmlGregorianCalendarModule() {
        this.addSerializer(XMLGregorianCalendar.class, new XmlGregorianCalendarSerializer());
        this.addDeserializer(XMLGregorianCalendar.class, new XmlGregorianCalendarDeserializer());
    }

}
