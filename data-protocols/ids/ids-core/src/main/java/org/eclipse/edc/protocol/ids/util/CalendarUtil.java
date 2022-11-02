/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *
 */

package org.eclipse.edc.protocol.ids.util;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public final class CalendarUtil {

    public static XMLGregorianCalendar gregorianNow() {
        try {
            GregorianCalendar gregorianCalendar = GregorianCalendar.from(ZonedDateTime.now());
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an XMLGregorianCalendar from an epoch seconds timestamp.
     *
     * @param timestamp the timestamp.
     * @return XMLGregorianCalendar representation of the timestamp.
     * @throws DatatypeConfigurationException if the timestamp cannot be parsed.
     */
    public static XMLGregorianCalendar gregorianFromEpochSeconds(long timestamp) throws DatatypeConfigurationException {
        var gregCal = new GregorianCalendar();
        gregCal.setTime(new Date(timestamp * 1000));
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregCal);
    }
}
