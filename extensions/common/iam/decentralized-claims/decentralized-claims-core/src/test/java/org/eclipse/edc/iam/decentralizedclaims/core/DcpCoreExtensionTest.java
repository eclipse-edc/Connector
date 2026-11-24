/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.decentralizedclaims.core.defaults.InMemoryCredentialVerifierRegistry;
import org.eclipse.edc.iam.decentralizedclaims.service.DcpIdentityService;
import org.eclipse.edc.iam.decentralizedclaims.service.verification.MultiFormatPresentationVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifierRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.HasSubjectRule;
import org.eclipse.edc.token.rules.JtiUnusedValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier;
import org.eclipse.edc.verifiablecredentials.jwt.Vcdm20JosePresentationVerifier;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.SubJwkIsNullRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.TokenNotNullRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.iam.decentralizedclaims.core.DcpCoreExtension.DCP_SELF_ISSUED_TOKEN_CONTEXT;
import static org.eclipse.edc.iam.decentralizedclaims.core.DcpCoreExtension.PARTICIPANT_DID;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.JWT_VC_TOKEN_CONTEXT;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DcpCoreExtensionTest {

    private static final String CLEANUP_PERIOD = "edc.sql.store.jti.cleanup.period";
    private final JtiValidationStore storeMock = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final TokenValidationRulesRegistry rulesRegistry = new TokenValidationRulesRegistryImpl();
    private final CredentialVerifierRegistry credentialVerifierRegistry = new InMemoryCredentialVerifierRegistry();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(SecureTokenService.class, mock());
        context.registerService(TypeManager.class, new JacksonTypeManager());
        context.registerService(JtiValidationStore.class, storeMock);
        context.registerService(ExecutorInstrumentation.class, ExecutorInstrumentation.noop());
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
        context.registerService(TokenValidationRulesRegistry.class, rulesRegistry);
        context.registerService(CredentialVerifierRegistry.class, credentialVerifierRegistry);
    }

    @Test
    void initialize_shouldRegisterJwtVerifiersOnly(DcpCoreExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        assertThat(credentialVerifierRegistry.getVerifiers())
                .extracting(CredentialVerifier::getClass)
                .containsExactly(JwtPresentationVerifier.class, Vcdm20JosePresentationVerifier.class);
    }

    @Test
    void presentationVerifier_shouldRejectLinkedDataPresentation_byDefault(ServiceExtensionContext context, ObjectFactory objectFactory) {
        context.registerService(TokenValidationService.class, new TokenValidationServiceImpl());
        var extension = objectFactory.constructInstance(DcpCoreExtension.class);
        extension.initialize(context);
        var verifier = extension.createPresentationVerifier(context);

        var ldpPresentation = """
                {
                  "@context": ["https://www.w3.org/2018/credentials/v1"],
                  "type": ["VerifiablePresentation"],
                  "holder": "did:web:holder"
                }
                """;
        var result = verifier.verifyPresentation(new VerifiablePresentationContainer(ldpPresentation, CredentialFormat.VC1_0_LD, null), "did:web:verifier");

        assertThat(result).isFailed().detail().contains("No verifier could handle the input data");
    }

    @Test
    void presentationVerifier_shouldUseVerifiersRegisteredLater(DcpCoreExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);
        var verifier = extension.createPresentationVerifier(context);
        CredentialVerifier additional = mock();

        credentialVerifierRegistry.register(additional);

        assertThat(verifier).isInstanceOfSatisfying(MultiFormatPresentationVerifier.class,
                v -> assertThat(v.getVerifiers()).contains(additional));
    }

    @Test
    void verifyCorrectService(TestExtensionContext context, ObjectFactory objectFactory) {
        var is = objectFactory.constructInstance(DcpCoreExtension.class).createIdentityService(context);
        assertThat(is).isInstanceOf(DcpIdentityService.class);
    }

    // unfortunately, this cannot be a parameterized test, because then we'd have 2 competing parameter resolvers: DependencyInjectionExtension and
    // the Parameterized test method resolver
    @Test
    void verifyCorrectRules_jtiValidationActive(TestExtensionContext context, ObjectFactory objectFactory) {
        var oldConfig = context.getConfig();
        var newConfig = new HashMap<>(oldConfig.getEntries());
        newConfig.put("edc.iam.accesstoken.jti.validation", "true");
        context.setConfig(ConfigFactory.fromMap(newConfig));

        var expectedRules = Arrays.asList(IssuerEqualsSubjectRule.class,
                SubJwkIsNullRule.class,
                ExpirationIssuedAtValidationRule.class,
                TokenNotNullRule.class,
                JtiUnusedValidationRule.class,
                NotBeforeValidationRule.class);

        var extension = objectFactory.constructInstance(DcpCoreExtension.class);
        extension.initialize(context);
        assertThat(rulesRegistry.getRules(DCP_SELF_ISSUED_TOKEN_CONTEXT))
                .extracting(TokenValidationRule::getClass)
                .containsExactlyInAnyOrderElementsOf(expectedRules);
    }

    @Test
    void verifyCorrectRules_jtiValidationNotActive(TestExtensionContext context, ObjectFactory objectFactory) {
        var oldConfig = context.getConfig();
        var newConfig = new HashMap<>(oldConfig.getEntries());
        newConfig.put("edc.iam.accesstoken.jti.validation", "false");
        context.setConfig(ConfigFactory.fromMap(newConfig));

        var expectedRules = Arrays.asList(IssuerEqualsSubjectRule.class,
                SubJwkIsNullRule.class,
                ExpirationIssuedAtValidationRule.class,
                TokenNotNullRule.class,
                // JtiUnusedValidationRule.class, should not be present when jti validation is disabled
                NotBeforeValidationRule.class);

        var extension = objectFactory.constructInstance(DcpCoreExtension.class);
        extension.initialize(context);
        assertThat(rulesRegistry.getRules(DCP_SELF_ISSUED_TOKEN_CONTEXT))
                .extracting(TokenValidationRule::getClass)
                .containsExactlyInAnyOrderElementsOf(expectedRules);

    }

    @Test
    void verifyCorrectRules_credential(DcpCoreExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        assertThat(rulesRegistry.getRules(JWT_VC_TOKEN_CONTEXT))
                .extracting(TokenValidationRule::getClass)
                .containsExactlyInAnyOrder(HasSubjectRule.class, IssuerKeyIdValidationRule.class);
    }

    @Test
    void assertReaperThreadRunning(TestExtensionContext context, ObjectFactory objectFactory) {
        var config = ConfigFactory.fromMap(Map.of(
                PARTICIPANT_DID, "did:web:test",
                CLEANUP_PERIOD, "1"
        ));
        context.setConfig(config);
        var extension = objectFactory.constructInstance(DcpCoreExtension.class);
        extension.initialize(context);
        extension.start();

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> verify(storeMock, atLeastOnce()).deleteExpired());
    }

    @Test
    void assertReaperThreadKeepsRunning_whenDeletingFails(TestExtensionContext context, ObjectFactory objectFactory) {
        when(storeMock.deleteExpired())
                .thenThrow(new EdcPersistenceException("database not reachable"))
                .thenReturn(StoreResult.success(0));
        var config = ConfigFactory.fromMap(Map.of(
                PARTICIPANT_DID, "did:web:test",
                CLEANUP_PERIOD, "1"
        ));
        context.setConfig(config);
        var extension = objectFactory.constructInstance(DcpCoreExtension.class);
        extension.initialize(context);
        extension.start();

        await().atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> verify(storeMock, atLeast(2)).deleteExpired());
        extension.shutdown();
    }

    @Test
    void shouldFallbackToParticipantId_whenDidNotSet(TestExtensionContext context, ObjectFactory objectFactory) {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.participant.id", "did:web:test",
                CLEANUP_PERIOD, "1"
        ));
        context.setConfig(config);

        objectFactory.constructInstance(DcpCoreExtension.class);
    }
}
