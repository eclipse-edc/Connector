/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.partner.cel;

import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerService;
import org.eclipse.edc.policy.cel.engine.CelExpressionEngineImpl;
import org.eclipse.edc.policy.cel.function.CelFunction;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistryImpl;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerCelFunctionsTest {

    private static final String PARTICIPANT_CONTEXT_ID = "pc";
    private static final String AGENT_ID = "did:web:acme";

    private final CelExpressionStore store = mock();
    private final PartnerService partnerService = mock();
    private final CelFunctionRegistryImpl functionRegistry = new CelFunctionRegistryImpl();
    private final CelExpressionEngineImpl engine = new CelExpressionEngineImpl(new NoopTransactionContext(), store, mock(), functionRegistry);

    PartnerCelFunctionsTest() {
        PartnerCelFunctions.functions(partnerService).forEach(functionRegistry::registerFunction);
    }

    @BeforeEach
    void setUp() {
        var acme = partner("acme", AGENT_ID, Set.of("gold", "eu"), Map.of("businessId", "BID-A", "tier", 1L));
        when(partnerService.findByIdentity(any(), any())).thenReturn(ServiceResult.notFound("missing"));
        when(partnerService.findById(any(), any())).thenReturn(ServiceResult.notFound("missing"));
        when(partnerService.findByIdentity(PARTICIPANT_CONTEXT_ID, AGENT_ID)).thenReturn(ServiceResult.success(acme));
        when(partnerService.findById(PARTICIPANT_CONTEXT_ID, "acme")).thenReturn(ServiceResult.success(acme));
        when(partnerService.search(any())).thenAnswer(a -> {
            QuerySpec spec = a.getArgument(0);
            var matches = spec.getFilterExpression().contains(criterion("properties.businessId", "=", "BID-A"));
            return ServiceResult.success(matches ? List.of(acme) : List.of());
        });
    }

    static Stream<Arguments> expressions() {
        return Stream.of(
                arguments("ctx.partners.byIdentity(ctx.agent.id).inGroup('gold')", true),
                arguments("ctx.partners.byIdentity(ctx.agent.id).inGroup('silver')", false),
                arguments("'eu' in ctx.partners.byIdentity(ctx.agent.id).groups", true),
                arguments("ctx.partners.byIdentity(ctx.agent.id).groups().size() == 2", true),
                arguments("ctx.partners.byIdentity(ctx.agent.id).found()", true),
                arguments("ctx.partners.byIdentity(ctx.agent.id).properties.businessId == 'BID-A'", true),
                arguments("ctx.partners.byIdentity(ctx.agent.id).properties.tier == 1", true),
                arguments("ctx.partners.byIdentity(ctx.agent.id).name == 'Partner acme'", true),
                arguments("ctx.partners.byId('acme').identity == 'did:web:acme'", true),
                arguments("ctx.partners.byId('nope').found()", false),
                // unknown counterparties never abort the evaluation
                arguments("ctx.partners.byIdentity('did:web:unknown').found()", false),
                arguments("ctx.partners.byIdentity('did:web:unknown').inGroup('gold')", false),
                arguments("ctx.partners.byIdentity('did:web:unknown').groups().size() == 0", true),
                arguments("ctx.partners.byIdentity('did:web:unknown').identity == 'did:web:unknown'", true),
                // property queries
                arguments("ctx.partners.query({'businessId': 'BID-A'}).size() > 0", true),
                arguments("ctx.partners.query({'businessId': 'BID-A'}).exists(p, p.inGroup('gold'))", true),
                arguments("ctx.partners.query({'businessId': 'nope'}).size() == 0", true),
                // the CEL representation composes with the standard macros
                arguments("ctx.partners.byIdentity(ctx.agent.id).groups.exists(g, g == 'gold')", true)
        );
    }

    @ParameterizedTest
    @MethodSource("expressions")
    void evaluates(String expression, boolean expected) {
        var result = evaluate(expression);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isEqualTo(expected);
    }

    @Test
    void query_shouldScopeToParticipantContext_andCapResults() {
        evaluate("ctx.partners.query({'businessId': 'BID-A'}).size() > 0");

        verify(partnerService).search(argThat(spec -> spec.getFilterExpression().contains(filterByParticipantContextId(PARTICIPANT_CONTEXT_ID)) &&
                spec.getFilterExpression().contains(criterion("properties.businessId", "=", "BID-A")) &&
                spec.getLimit() == PartnerCelFunctions.MAX_QUERY_RESULTS));
    }

    @Test
    void shouldNotFindAnything_whenHandleHasNoParticipantContext() {
        var params = Map.<String, Object>of("agent", Map.of("id", AGENT_ID), "partners", Map.of());

        var result = evaluate("ctx.partners.byIdentity(ctx.agent.id).found() || ctx.partners.query({'businessId': 'BID-A'}).size() > 0", params);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isFalse();
    }

    @Test
    void overloadIdsAreUnique() {
        assertThat(PartnerCelFunctions.functions(partnerService)).extracting(CelFunction::overloadId).doesNotHaveDuplicates();
    }

    private ServiceResult<Boolean> evaluate(String expression) {
        return evaluate(expression, Map.of(
                "agent", Map.of("id", AGENT_ID, "claims", Map.of(), "attributes", Map.of()),
                "partners", Map.of("participantContextId", PARTICIPANT_CONTEXT_ID)));
    }

    private ServiceResult<Boolean> evaluate(String expression, Map<String, Object> params) {
        when(store.query(any())).thenReturn(List.of(CelExpression.Builder.newInstance()
                .id("id").leftOperand("test").expression(expression).description("d").build()));
        return engine.evaluateExpression("test", Operator.EQ, "null", params);
    }

    private Partner partner(String id, String identity, Set<String> groups, Map<String, Object> properties) {
        return Partner.Builder.newInstance()
                .id(id).participantContextId(PARTICIPANT_CONTEXT_ID).identity(identity).name("Partner " + id)
                .groupIds(groups).properties(properties)
                .build();
    }
}
