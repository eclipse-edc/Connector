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

package org.eclipse.dataspaceconnector.serializer.calendar;

import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import org.eclipse.dataspaceconnector.ids.core.policy.IdsConstraintImpl;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.domain.DefaultValues;
import org.eclipse.dataspaceconnector.serializer.JsonldSerDes;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class XmlGregorianCalendarModuleTest {

    private JsonldSerDes serDes;

    @BeforeEach
    void setUp() {
        serDes = new JsonldSerDes(mock(Monitor.class));
    }

    @Test
    void serializeAndDeserializeDate() throws IOException {
        var xmlGregorian = CalendarUtil.gregorianNow();

        var serialized = serDes.serialize(xmlGregorian);
        var xmlGregorianDeserialized = serDes.deserialize(serialized, XMLGregorianCalendar.class);
        var serialized2 = serDes.serialize(xmlGregorianDeserialized);

        assertThat(xmlGregorian).isEqualTo(xmlGregorianDeserialized);
        assertThat(serialized).isEqualTo(serialized2);
    }

    @Test
    void serializeAndDeserializeAgreement() throws IOException {
        serDes.setContext(DefaultValues.CONTEXT);
        serDes.setSubtypes(IdsConstraintImpl.class);

        var xmlGregorian = CalendarUtil.gregorianNow();
        var agreement = new ContractAgreementBuilder()
                ._contractDate_(xmlGregorian)
                ._contractStart_(xmlGregorian)
                ._contractEnd_(xmlGregorian)
                .build();

        var serialized = serDes.serialize(agreement);
        var agreementDeserialized = serDes.deserialize(serialized, ContractAgreement.class);
        var serialized2 = serDes.serialize(agreementDeserialized);

        assertThat(agreement).isEqualTo(agreementDeserialized);
        assertThat(serialized).isEqualTo(serialized2);
    }

}
