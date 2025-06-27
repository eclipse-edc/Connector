/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.tck.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.test.e2e.tck.TckTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry.WILDCARD;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Asserts the correct functionality of the presentation flow according to the Technology Compatibility Kit (TCK).
 * <p>
 * IdentityHub is started in an in-mem runtime, the TCK is started in another runtime, and executes its test cases against
 * IdentityHubs Presentation API.
 *
 * @see <a href="https://github.com/eclipse-dataspacetck/dcp-tck">Eclipse Dataspace TCK - DCP</a>
 */
@TckTest
public class DcpPresentationFlowTest {
    private static final int CALLBACK_PORT = getFreePort();

    private static final String PROTOCOL_API_PATH = "/api/protocol";
    private static final String PROTOCOL_API_PORT = String.valueOf(getFreePort());
    private static final SecureTokenService STS_MOCK = mock();
    private static final int DID_SERVER_PORT = getFreePort();
    private static final String VERIFIER_DID = "did:web:localhost%%3A%s:verifier".formatted(DID_SERVER_PORT);
    @RegisterExtension
    static final RuntimePerClassExtension EDC_RUNTIME_EXTENSIONS = new RuntimePerClassExtension(
            new EmbeddedRuntime("Connector-under-test", ":dist:bom:controlplane-dcp-bom")
                    .registerServiceMock(SecureTokenService.class, STS_MOCK)
                    .registerSystemExtension(ServiceExtension.class, new DefaultScopeFunctionExtension())
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                            "edc.iam.accesstoken.jti.validation", "true",
                            "edc.iam.did.web.use.https", "false",
                            "web.http.port", String.valueOf(getFreePort()),
                            // use DSP endpoints as trigger endpoint
                            "web.http.protocol.path", PROTOCOL_API_PATH,
                            "web.http.protocol.port", PROTOCOL_API_PORT,
                            "edc.iam.issuer.id", VERIFIER_DID,
                            "edc.iam.sts.oauth.token.url", "https://example.com/token",
                            "edc.iam.sts.oauth.client.id", "test-client-id",
                            "edc.iam.sts.oauth.client.secret.alias", "test-secret-alias"
                    )))
    );
    private ClientAndServer server;
    private ECKey verifierKey;

    @AfterEach
    void teardown() {
        if (server.hasStarted()) {
            server.stop();
        }
    }

    @BeforeEach
    void setup(TrustedIssuerRegistry trustedIssuerRegistry) throws JOSEException {
        verifierKey = new ECKeyGenerator(Curve.P_256).keyID(VERIFIER_DID + "#verifier-key1").generate();

        trustedIssuerRegistry.register(new Issuer("did:web:localhost%%3A%s:issuer".formatted(CALLBACK_PORT), Map.of()), WILDCARD);

        // start mocked DID server
        server = ClientAndServer.startClientAndServer(DID_SERVER_PORT);
        var didDocumentJson = createDidDocumentJson();
        server.when(request().withMethod("GET").withPath("/verifier/did.json"))
                .respond(response()
                                 .withHeader("Content-Type", "application/json")
                                 .withStatusCode(200)
                                 .withBody(didDocumentJson));

        when(STS_MOCK.createToken(anyMap(), isNull()))
                .thenAnswer(i -> {
                    Map<String, Object> claims = i.getArgument(0);

                    var hdr = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(verifierKey.getKeyID()).build();

                    // workaround for #4991
                    claims = new HashMap<>(claims);
                    claims.put("iat", Instant.now().getEpochSecond());
                    claims.put("exp", Instant.now().plusSeconds(5 * 60).getEpochSecond());
                    claims.put("nbf", Instant.now().getEpochSecond());

                    var claimsSet = new JWTClaimsSet.Builder(JWTClaimsSet.parse(claims))
                                            .jwtID(UUID.randomUUID().toString())
                                            .build();
                    var jwt = new SignedJWT(hdr, claimsSet);
                    jwt.sign(new ECDSASigner(verifierKey));
                    var tr = TokenRepresentation.Builder.newInstance().token(jwt.serialize()).build();
                    return success(tr);
                });
    }

    private String createDidDocumentJson() {
        var ddoc = DidDocument.Builder.newInstance()
                           .id(VERIFIER_DID)
                           .verificationMethod(List.of(
                                   VerificationMethod.Builder.newInstance()
                                           .type("assertionMethod")
                                           .controller(VERIFIER_DID)
                                           .publicKeyJwk(verifierKey.toJSONObject())
                                           .id(verifierKey.getKeyID())
                                           .build()
                           ))
                           .service(List.of(new Service(UUID.randomUUID().toString(), "CredentialService", "https://example.com/credentialservice")))
                           .build();
        try {
            return new ObjectMapper().writeValueAsString(ddoc);
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    @DisplayName("Run TCK Presentation Flow tests")
    @Test
    void runPresentationFlowTests() {
        var monitor = new ConsoleMonitor(true, true);

        var triggerPath = PROTOCOL_API_PATH + "/2024/1/catalog/request"; // todo: update to 2025 as soon as that is the default
        var holderDid = "did:web:localhost%3A" + CALLBACK_PORT + ":holder";
        var thirdPartyDid = "did:web:localhost%3A" + CALLBACK_PORT + ":thirdparty";
        var baseCallbackUrl = "http://localhost:%s".formatted(CALLBACK_PORT);
        var result = TckRuntime.Builder.newInstance()
                             .properties(Map.of(
                                     "dataspacetck.callback.address", baseCallbackUrl,
                                     "dataspacetck.launcher", "org.eclipse.dataspacetck.dcp.system.DcpSystemLauncher",
                                     "dataspacetck.did.verifier", VERIFIER_DID,
                                     "dataspacetck.did.holder", holderDid,
                                     "dataspacetck.did.thirdparty", thirdPartyDid,
                                     "dataspacetck.vpp.trigger.endpoint", "http://localhost:%s%s".formatted(PROTOCOL_API_PORT, triggerPath)
                             ))
                             .monitor(monitor)
                             .addPackage("org.eclipse.dataspacetck.dcp.verification.presentation.verifier")
                             .build()
                             .execute();

        monitor.enableBold().message("DCP Tests done: %s succeeded, %s failed".formatted(
                result.getTestsSucceededCount(), result.getTotalFailureCount()
        )).resetMode();

        if (!result.getFailures().isEmpty()) {
            var failures = result.getFailures().stream()
                                   .map(f -> "- " + f.getTestIdentifier().getDisplayName() + " (" + f.getException() + ")")
                                   .collect(Collectors.joining("\n"));
            Assertions.fail(result.getTotalFailureCount() + " TCK test cases failed:\n" + failures);
        }
    }

}
