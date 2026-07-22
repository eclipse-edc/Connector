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

package org.eclipse.edc.iam.decentralizedclaims.cel;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.cel.engine.CelExpressionEngineImpl;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistryImpl;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the VC helper functions through the real CEL engine, over the credential shape actually produced by
 * {@link VcClaimMapper}.
 */
class VcCelFunctionsTest {

    private final CelExpressionStore store = mock();
    private final CelFunctionRegistryImpl functionRegistry = new CelFunctionRegistryImpl();
    private final CelExpressionEngineImpl engine =
            new CelExpressionEngineImpl(new NoopTransactionContext(), store, mock(), functionRegistry);

    VcCelFunctionsTest() {
        VcCelFunctions.functions(Clock.systemUTC()).forEach(functionRegistry::registerFunction);
    }

    @Test
    void shortFormMatchesLongForm() {
        var longForm = """
                ctx.agent.claims.vc
                     .filter(c, c.type.exists(t, t == 'MembershipCredential'))
                     .exists(c, c.credentialSubject.exists(cs, cs.memberOf == 'Catena-X'))
                """;
        var shortForm = "ctx.agent.claims.vc.withType('MembershipCredential').hasClaim('memberOf', 'Catena-X')";

        assertThat(evaluate(longForm)).isTrue();
        assertThat(evaluate(shortForm)).isTrue();
    }

    static Stream<Arguments> helpers() {
        return Stream.of(
                arguments("ctx.agent.claims.vc.hasCredential('MembershipCredential')", true),
                arguments("ctx.agent.claims.vc.hasCredential('DataAccessCredential')", false),
                arguments("ctx.agent.claims.vc.withType('MembershipCredential').hasClaim('memberOf')", true),
                arguments("ctx.agent.claims.vc.withType('MembershipCredential').hasClaim('memberOf', 'Catena-X')", true),
                arguments("ctx.agent.claims.vc.withType('MembershipCredential').hasClaim('memberOf', 'Other')", false),
                arguments("ctx.agent.claims.vc.withType('MembershipCredential').claim('level') == 'gold'", true),
                arguments("ctx.agent.claims.vc.withIssuer('did:web:issuer').hasCredential('MembershipCredential')", true),
                arguments("ctx.agent.claims.vc.withIssuer('did:web:other').hasCredential('MembershipCredential')", false),
                arguments("ctx.agent.claims.vc.withContext('https://www.w3.org/2018/credentials/v1').hasCredential('MembershipCredential')", true),
                arguments("ctx.agent.claims.vc.withContext('https://example.org/unknown').hasCredential('MembershipCredential')", false),
                arguments("ctx.agent.claims.vc.exists(c, c.hasContext('https://www.w3.org/2018/credentials/v1'))", true),
                arguments("ctx.agent.claims.vc.valid().hasCredential('MembershipCredential')", true),
                arguments("ctx.agent.claims.vc.valid().hasCredential('ExpiredCredential')", false),
                arguments("ctx.agent.claims.vc.withType('MembershipCredential').claims('level').size() == 1", true),
                // dotted paths reach nested subject claims
                arguments("ctx.agent.claims.vc.hasClaim('degree.type', 'BachelorDegree')", true),
                arguments("ctx.agent.claims.vc.hasClaim('degree.missing', 'x')", false),
                // the subject id is exposed as a claim
                arguments("ctx.agent.claims.vc.hasClaim('id', 'did:web:subject')", true),
                // single-credential receiver composes with the standard macros
                arguments("ctx.agent.claims.vc.exists(c, c.hasType('MembershipCredential'))", true),
                arguments("ctx.agent.claims.vc.exists(c, c.hasClaim('memberOf', 'Catena-X'))", true),
                arguments("ctx.agent.claims.vc.exists(c, c.hasType('MembershipCredential') && c.valid())", true),
                arguments("ctx.agent.claims.vc.exists(c, c.hasType('ExpiredCredential') && c.valid())", false)
        );
    }

    @ParameterizedTest
    @MethodSource("helpers")
    void evaluatesHelpers(String expression, boolean expected) {
        assertThat(evaluate(expression)).isEqualTo(expected);
    }

    /**
     * Plain map access aborts evaluation on an absent key; the helpers must return false instead. This is the main
     * usability benefit over hand-written filter/exists expressions.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "ctx.agent.claims.vc.hasClaim('missingClaim', 'x')",
            "ctx.agent.claims.vc.hasClaim('missingClaim')",
            "ctx.agent.claims.vc.withType('NoSuchType').hasClaim('memberOf', 'Catena-X')",
            "ctx.agent.claims.vc.hasClaim('memberOf.nested.deep', 'x')"
    })
    void missingDataYieldsFalseRatherThanError(String expression) {
        var result = evaluateResult(expression);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isFalse();
    }

    @Test
    void plainMapAccessStillErrorsOnMissingKey() {
        // guards the contrast above: the helpers are what make this safe, not a change to CEL itself
        assertThat(evaluateResult("ctx.agent.claims.vc.exists(c, c.credentialSubject.exists(cs, cs.missing == 'x'))"))
                .isFailed();
    }

    /**
     * Credential claim names are often IRIs, which contain dots. The whole name must be tried as a literal key before
     * being interpreted as a path, otherwise such claims are unreachable.
     */
    @Test
    void resolvesClaimNameContainingDots() {
        var vc = VerifiableCredential.Builder.newInstance()
                .id("credential-3")
                .types(List.of("VerifiableCredential", "NamespacedCredential"))
                .issuer(new Issuer("did:web:issuer", Map.of()))
                .issuanceDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .claim("https://w3id.org/edc/v0.0.1/ns/level", "gold")
                        .build())
                .build();
        var params = paramsFor(List.of(vc));

        var result = evaluateResult("ctx.agent.claims.vc.hasClaim('https://w3id.org/edc/v0.0.1/ns/level', 'gold')", params);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isTrue();
    }

    /**
     * Matches {@code IsInValidityPeriod}, the rule the verification pipeline applies: the expiration instant itself is
     * still valid, and a credential whose issuance date lies in the future is not.
     */
    @Test
    void validityBoundariesMatchVerificationPipeline() {
        var now = Instant.parse("2026-01-01T00:00:00Z");
        var clock = Clock.fixed(now, java.time.ZoneOffset.UTC);
        var registry = new CelFunctionRegistryImpl();
        VcCelFunctions.functions(clock).forEach(registry::registerFunction);
        var fixedEngine = new CelExpressionEngineImpl(new NoopTransactionContext(), store, mock(), registry);

        var expiringNow = credential("ExpiringNow", now.minus(1, ChronoUnit.DAYS), now);
        var notYetValid = credential("NotYetValid", now.plus(1, ChronoUnit.DAYS), null);
        var params = paramsFor(List.of(expiringNow, notYetValid));

        when(store.query(any())).thenReturn(List.of(CelExpression.Builder.newInstance().id("id").leftOperand("test")
                .expression("ctx.agent.claims.vc.valid().hasCredential('ExpiringNow')").description("d").build()));
        assertThat(fixedEngine.evaluateExpression("test", Operator.EQ, "null", params).getContent()).isTrue();

        when(store.query(any())).thenReturn(List.of(CelExpression.Builder.newInstance().id("id").leftOperand("test")
                .expression("ctx.agent.claims.vc.valid().hasCredential('NotYetValid')").description("d").build()));
        assertThat(fixedEngine.evaluateExpression("test", Operator.EQ, "null", params).getContent()).isFalse();
    }

    @Test
    void handlesAgentWithoutCredentials() {
        var params = Map.<String, Object>of("agent", Map.of("id", "agent-1", "claims", Map.of("vc", List.of())));

        var result = evaluateResult("ctx.agent.claims.vc.hasCredential('MembershipCredential')", params);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isFalse();
    }

    private boolean evaluate(String expression) {
        var result = evaluateResult(expression);
        assertThat(result).isSucceeded();
        return result.getContent();
    }

    private ServiceResult<Boolean> evaluateResult(String expression) {
        return evaluateResult(expression, params());
    }

    private ServiceResult<Boolean> evaluateResult(String expression, Map<String, Object> params) {
        when(store.query(any())).thenReturn(List.of(CelExpression.Builder.newInstance()
                .id("id").leftOperand("test").expression(expression).description("d").build()));
        return engine.evaluateExpression("test", Operator.EQ, "null", params);
    }

    /**
     * Builds the context exactly as it is produced at runtime, by running the real {@link VcClaimMapper}.
     */
    private Map<String, Object> params() {
        return paramsFor(credentials());
    }

    private Map<String, Object> paramsFor(List<VerifiableCredential> credentials) {
        var agent = new ParticipantAgent("agent-1", Map.of("vc", credentials), Map.of());
        var vc = new VcClaimMapper().mapClaim(agent);
        return Map.of("agent", Map.of("id", "agent-1", "claims", Map.of(vc.name(), vc.value())));
    }

    private VerifiableCredential credential(String type, Instant issuance, Instant expiration) {
        var builder = VerifiableCredential.Builder.newInstance()
                .id("credential-" + type)
                .types(List.of("VerifiableCredential", type))
                .issuer(new Issuer("did:web:issuer", Map.of()))
                .issuanceDate(issuance)
                .credentialSubject(CredentialSubject.Builder.newInstance().claim("memberOf", "Catena-X").build());
        if (expiration != null) {
            builder.expirationDate(expiration);
        }
        return builder.build();
    }

    private List<VerifiableCredential> credentials() {
        var membership = VerifiableCredential.Builder.newInstance()
                .id("credential-1")
                .contexts(List.of("https://www.w3.org/2018/credentials/v1"))
                .types(List.of("VerifiableCredential", "MembershipCredential"))
                .issuer(new Issuer("did:web:issuer", Map.of()))
                .issuanceDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .expirationDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("did:web:subject")
                        .claim("memberOf", "Catena-X")
                        .claim("level", "gold")
                        .claim("degree", Map.of("type", "BachelorDegree"))
                        .build())
                .build();

        var expired = VerifiableCredential.Builder.newInstance()
                .id("credential-2")
                .types(List.of("VerifiableCredential", "ExpiredCredential"))
                .issuer(new Issuer("did:web:issuer", Map.of()))
                .issuanceDate(Instant.now().minus(10, ChronoUnit.DAYS))
                .expirationDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .credentialSubject(CredentialSubject.Builder.newInstance().claim("memberOf", "Catena-X").build())
                .build();

        return List.of(membership, expired);
    }
}
