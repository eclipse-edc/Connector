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
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.ContractTransformerInput;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IdsContractAgreementToContractAgreementTransformerTest {
    private static final URI AGREEMENT_ID = URI.create("urn:contractagreement:456uz984390236s");
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");

    private static final URI CONSUMER_URI = URI.create("https://provider.com/");
    private static final XMLGregorianCalendar CONTRACT_START = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.JANUARY, 1));
    private static final XMLGregorianCalendar CONTRACT_END = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.FEBRUARY, 2));
    private static final XMLGregorianCalendar SIGNING_DATE = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.FEBRUARY, 2));

    private IdsContractAgreementToContractAgreementTransformer transformer;

    private de.fraunhofer.iais.eis.Permission idsPermission;
    private de.fraunhofer.iais.eis.Prohibition idsProhibition;
    private de.fraunhofer.iais.eis.Duty idsDuty;
    private TransformerContext context;
    private ContractTransformerInput input;

    @BeforeEach
    void setUp() {
        transformer = new IdsContractAgreementToContractAgreementTransformer();
        idsPermission = new de.fraunhofer.iais.eis.PermissionBuilder().build();
        idsProhibition = new de.fraunhofer.iais.eis.ProhibitionBuilder().build();
        idsDuty = new de.fraunhofer.iais.eis.DutyBuilder().build();
        // mocks
        var idsContractAgreement = new de.fraunhofer.iais.eis.ContractAgreementBuilder(AGREEMENT_ID)
                ._provider_(PROVIDER_URI)
                ._consumer_(CONSUMER_URI)
                ._permission_(new ArrayList<>(Collections.singletonList(idsPermission)))
                ._prohibition_(new ArrayList<>(Collections.singletonList(idsProhibition)))
                ._obligation_(new ArrayList<>(Collections.singletonList(idsDuty)))
                ._contractStart_(CONTRACT_START)
                ._contractEnd_(CONTRACT_END)
                ._contractDate_(SIGNING_DATE)
                .build();
        Asset asset = Asset.Builder.newInstance().build();
        input = ContractTransformerInput.Builder.newInstance().contract(idsContractAgreement).asset(asset).build();
        context = mock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(input, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        Permission edcPermission = mock(Permission.class);
        Prohibition edcProhibition = mock(Prohibition.class);
        Duty edcObligation = mock(Duty.class);

        when(context.transform(eq(idsPermission), eq(Permission.class))).thenReturn(edcPermission);
        when(context.transform(eq(idsProhibition), eq(Prohibition.class))).thenReturn(edcProhibition);
        when(context.transform(eq(idsDuty), eq(Duty.class))).thenReturn(edcObligation);

        var result = transformer.transform(input, context);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getPolicy());
        var policy = result.getPolicy();
        Assertions.assertEquals(1, policy.getObligations().size());
        Assertions.assertEquals(edcObligation, policy.getObligations().get(0));
        Assertions.assertEquals(1, policy.getPermissions().size());
        Assertions.assertEquals(edcPermission, policy.getPermissions().get(0));
        Assertions.assertEquals(1, policy.getProhibitions().size());
        Assertions.assertEquals(edcProhibition, policy.getProhibitions().get(0));
        verify(context).transform(eq(idsPermission), eq(Permission.class));
        verify(context).transform(eq(idsProhibition), eq(Prohibition.class));
        verify(context).transform(eq(idsDuty), eq(Duty.class));
    }
}
