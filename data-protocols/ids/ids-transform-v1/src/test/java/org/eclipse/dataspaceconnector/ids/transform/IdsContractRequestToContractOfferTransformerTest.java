/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
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

public class IdsContractRequestToContractOfferTransformerTest {
    private static final URI REQUEST_ID = URI.create("urn:contractrequest:456uz984390236s");
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");

    private static final URI CONSUMER_URI = URI.create("https://provider.com/");
    private static final XMLGregorianCalendar CONTRACT_START = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.JANUARY, 1));
    private static final XMLGregorianCalendar CONTRACT_END = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.FEBRUARY, 2));

    // subject
    private IdsContractRequestToContractOfferTransformer transformer;

    // mocks
    private de.fraunhofer.iais.eis.ContractRequest idsContractRequest;
    private de.fraunhofer.iais.eis.Permission idsPermission;
    private de.fraunhofer.iais.eis.Prohibition idsProhibition;
    private de.fraunhofer.iais.eis.Duty idsDuty;
    private TransformerContext context;
    private ContractTransformerInput input;

    @BeforeEach
    void setUp() {
        transformer = new IdsContractRequestToContractOfferTransformer();
        idsPermission = new de.fraunhofer.iais.eis.PermissionBuilder().build();
        idsProhibition = new de.fraunhofer.iais.eis.ProhibitionBuilder().build();
        idsDuty = new de.fraunhofer.iais.eis.DutyBuilder().build();
        idsContractRequest = new de.fraunhofer.iais.eis.ContractRequestBuilder(REQUEST_ID)
                ._provider_(PROVIDER_URI)
                ._provider_(CONSUMER_URI)
                ._permission_(new ArrayList<>(Collections.singletonList(idsPermission)))
                ._prohibition_(new ArrayList<>(Collections.singletonList(idsProhibition)))
                ._obligation_(new ArrayList<>(Collections.singletonList(idsDuty)))
                ._contractStart_(CONTRACT_START)
                ._contractEnd_(CONTRACT_END)
                .build();
        var asset = Asset.Builder.newInstance().build();
        input = ContractTransformerInput.Builder.newInstance().contract(idsContractRequest).asset(asset).build();
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
            transformer.transform(input, null);
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
        var result = transformer.transform(input, context);

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
