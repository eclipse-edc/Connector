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
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class IdsContractOfferToContractOfferTransformerTest {
    private static final URI OFFER_ID = URI.create("urn:offer:456uz984390236s");
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");

    private static final URI CONSUMER_URI = URI.create("https://provider.com/");
    private static final XMLGregorianCalendar CONTRACT_START = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.JANUARY, 1));
    private static final XMLGregorianCalendar CONTRACT_END = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.FEBRUARY, 2));

    // subject
    private IdsContractOfferToContractOfferTransformer transformer;

    // mocks
    private de.fraunhofer.iais.eis.ContractOffer idsContractOffer;
    private de.fraunhofer.iais.eis.Permission idsPermission;
    private de.fraunhofer.iais.eis.Prohibition idsProhibition;
    private de.fraunhofer.iais.eis.Duty idsDuty;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsContractOfferToContractOfferTransformer();
        idsPermission = new de.fraunhofer.iais.eis.PermissionBuilder().build();
        idsProhibition = new de.fraunhofer.iais.eis.ProhibitionBuilder().build();
        idsDuty = new de.fraunhofer.iais.eis.DutyBuilder().build();
        idsContractOffer = new de.fraunhofer.iais.eis.ContractOfferBuilder(OFFER_ID)
                ._provider_(PROVIDER_URI)
                ._consumer_(CONSUMER_URI)
                ._permission_(new ArrayList<>(Collections.singletonList(idsPermission)))
                ._prohibition_(new ArrayList<>(Collections.singletonList(idsProhibition)))
                ._obligation_(new ArrayList<>(Collections.singletonList(idsDuty)))
                ._contractStart_(CONTRACT_START)
                ._contractEnd_(CONTRACT_END)
                .build();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(idsContractOffer, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare

        Permission edcPermission = EasyMock.createMock(Permission.class);
        Prohibition edcProhibition = EasyMock.createMock(Prohibition.class);
        Duty edcObligation = EasyMock.createMock(Duty.class);

        EasyMock.expect(context.transform(EasyMock.eq(idsPermission), EasyMock.eq(Permission.class))).andReturn(edcPermission);
        EasyMock.expect(context.transform(EasyMock.eq(idsProhibition), EasyMock.eq(Prohibition.class))).andReturn(edcProhibition);
        EasyMock.expect(context.transform(EasyMock.eq(idsDuty), EasyMock.eq(Duty.class))).andReturn(edcObligation);
        EasyMock.expectLastCall().atLeastOnce();

        // record
        EasyMock.replay(context);

        // invoke
        var result = transformer.transform(idsContractOffer, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getPolicy());
        var policy = result.getPolicy();
        Assertions.assertEquals(1, policy.getObligations().size());
        Assertions.assertEquals(edcObligation, policy.getObligations().get(0));
        Assertions.assertEquals(1, policy.getPermissions().size());
        Assertions.assertEquals(edcPermission, policy.getPermissions().get(0));
        Assertions.assertEquals(1, policy.getProhibitions().size());
        Assertions.assertEquals(edcProhibition, policy.getProhibitions().get(0));
    }
}
