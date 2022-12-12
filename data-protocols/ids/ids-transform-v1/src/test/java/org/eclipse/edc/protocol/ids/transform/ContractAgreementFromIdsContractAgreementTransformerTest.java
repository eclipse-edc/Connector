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

package org.eclipse.edc.protocol.ids.transform;

import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.transform.type.contract.ContractAgreementFromIdsContractAgreementTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContractAgreementFromIdsContractAgreementTransformerTest {

    private static final URI AGREEMENT_ID = URI.create("urn:contractagreement:456uz984390236s");
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");
    private static final URI CONSUMER_URI = URI.create("https://provider.com/");
    private static final XMLGregorianCalendar CONTRACT_START = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.JANUARY, 1));
    private static final XMLGregorianCalendar CONTRACT_END = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.FEBRUARY, 2));
    private static final XMLGregorianCalendar SIGNING_DATE = DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar(2021, Calendar.FEBRUARY, 2));

    private de.fraunhofer.iais.eis.Permission idsPermission;
    private de.fraunhofer.iais.eis.Prohibition idsProhibition;
    private de.fraunhofer.iais.eis.Duty idsDuty;
    private final TransformerContext context = mock(TransformerContext.class);

    private ContractAgreementFromIdsContractAgreementTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new ContractAgreementFromIdsContractAgreementTransformer();
        idsPermission = new de.fraunhofer.iais.eis.PermissionBuilder()._target_(URI.create("urn:artifact:assetId")).build();
        idsProhibition = new de.fraunhofer.iais.eis.ProhibitionBuilder().build();
        idsDuty = new de.fraunhofer.iais.eis.DutyBuilder().build();
    }

    @Test
    void shouldReturnNull_ifInputIsNull() {
        var result = transformer.transform(null, context);

        assertThat(result).isNull();
    }

    @Test
    void shouldTransform() {
        var edcPermission = Permission.Builder.newInstance().target("assetId").build();
        var edcProhibition = mock(Prohibition.class);
        var edcObligation = mock(Duty.class);
        when(context.transform(eq(idsPermission), eq(Permission.class))).thenReturn(edcPermission);
        when(context.transform(eq(idsProhibition), eq(Prohibition.class))).thenReturn(edcProhibition);
        when(context.transform(eq(idsDuty), eq(Duty.class))).thenReturn(edcObligation);

        var contractAgreement = createFullContractAgreementBuilder().build();

        var result = transformer.transform(contractAgreement, context);

        assertThat(result).isNotNull();
        assertThat(result.getContractAgreement().getAssetId()).isEqualTo("assetId");
        assertThat(result.getPolicy()).isNotNull().satisfies(policy -> {
            assertThat(policy.getObligations()).hasSize(1).containsExactly(edcObligation);
            assertThat(policy.getPermissions()).hasSize(1).containsExactly(edcPermission);
            assertThat(policy.getProhibitions()).hasSize(1).containsExactly(edcProhibition);
        });
        assertThat(result.getContractAgreement()).isNotNull();
        verify(context).transform(eq(idsPermission), eq(Permission.class));
        verify(context).transform(eq(idsProhibition), eq(Prohibition.class));
        verify(context).transform(eq(idsDuty), eq(Duty.class));
    }

    @Test
    void shouldReportProblem_whenThereAreNoPermissions() {
        var contractAgreement = createFullContractAgreementBuilder()
                ._permission_(emptyList())
                .build();

        var result = transformer.transform(contractAgreement, context);

        assertThat(result).isNull();
        verify(context).reportProblem(any());
    }

    private ContractAgreementBuilder createFullContractAgreementBuilder() {
        return new de.fraunhofer.iais.eis.ContractAgreementBuilder(AGREEMENT_ID)
                ._provider_(PROVIDER_URI)
                ._consumer_(CONSUMER_URI)
                ._permission_(List.of(idsPermission))
                ._prohibition_(List.of(idsProhibition))
                ._obligation_(List.of(idsDuty))
                ._contractStart_(CONTRACT_START)
                ._contractEnd_(CONTRACT_END)
                ._contractDate_(SIGNING_DATE);
    }
}
