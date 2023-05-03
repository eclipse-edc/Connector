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

package org.eclipse.edc.protocol.ids.transform;

import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.ContractRequest;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.spi.transform.ContractTransformerInput;
import org.eclipse.edc.protocol.ids.transform.type.contract.ContractOfferFromIdsContractOfferOrRequestTransformer;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ContractOfferFromIdsContractRequestTransformerTest {
    private static final URI REQUEST_ID = URI.create("urn:contractrequest:456uz984390236s");
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");

    private static final URI CONSUMER_URI = URI.create("https://provider.com/");
    private static final XMLGregorianCalendar CONTRACT_START = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.JANUARY, 1));
    private static final XMLGregorianCalendar CONTRACT_END = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.FEBRUARY, 2));

    private ContractOfferFromIdsContractOfferOrRequestTransformer transformer;

    private de.fraunhofer.iais.eis.Permission idsPermission;
    private de.fraunhofer.iais.eis.Prohibition idsProhibition;
    private de.fraunhofer.iais.eis.Duty idsDuty;
    private TransformerContext context;

    private ContractRequest idsContractRequest;
    private ContractOffer idsContractOffer;
    private Asset asset;

    @BeforeEach
    void setUp() {
        transformer = new ContractOfferFromIdsContractOfferOrRequestTransformer();
        idsPermission = new de.fraunhofer.iais.eis.PermissionBuilder().build();
        idsProhibition = new de.fraunhofer.iais.eis.ProhibitionBuilder().build();
        idsDuty = new de.fraunhofer.iais.eis.DutyBuilder().build();
        idsContractRequest = new de.fraunhofer.iais.eis.ContractRequestBuilder(REQUEST_ID)
                ._provider_(PROVIDER_URI)
                ._consumer_(CONSUMER_URI)
                ._permission_(new ArrayList<>(Collections.singletonList(idsPermission)))
                ._prohibition_(new ArrayList<>(Collections.singletonList(idsProhibition)))
                ._obligation_(new ArrayList<>(Collections.singletonList(idsDuty)))
                ._contractStart_(CONTRACT_START)
                ._contractEnd_(CONTRACT_END)
                .build();
        idsContractOffer = new de.fraunhofer.iais.eis.ContractOfferBuilder(REQUEST_ID)
                ._provider_(PROVIDER_URI)
                ._consumer_(CONSUMER_URI)
                ._permission_(new ArrayList<>(Collections.singletonList(idsPermission)))
                ._prohibition_(new ArrayList<>(Collections.singletonList(idsProhibition)))
                ._obligation_(new ArrayList<>(Collections.singletonList(idsDuty)))
                ._contractStart_(CONTRACT_START)
                ._contractEnd_(CONTRACT_END)
                .build();
        asset = Asset.Builder.newInstance().build();
        context = mock(TransformerContext.class);
    }

    @Test
    void testSuccessfulSimple_contractRequest() {
        var input = ContractTransformerInput.Builder.newInstance().contract(idsContractRequest).asset(asset).build();
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

    @Test
    void testSuccessfulSimple_contractOffer() {
        var input = ContractTransformerInput.Builder.newInstance().contract(idsContractOffer).asset(asset).build();
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
