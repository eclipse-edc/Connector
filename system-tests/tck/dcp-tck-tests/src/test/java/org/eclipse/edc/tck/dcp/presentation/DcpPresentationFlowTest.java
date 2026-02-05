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

package org.eclipse.edc.tck.dcp.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.tck.TckTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry.WILDCARD;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                            "edc.iam.dcp.scopes.membership.id", "membership-scope",
                            "edc.iam.dcp.scopes.membership.type", "DEFAULT",
                            "edc.iam.dcp.scopes.membership.value", "org.eclipse.dspace.dcp.vc.type:MembershipCredential:read"
                    )))
    );

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().port(DID_SERVER_PORT))
            .build();
    private ECKey verifierKey;


    @BeforeEach
    void setup(TrustedIssuerRegistry trustedIssuerRegistry) throws JOSEException {
        verifierKey = new ECKeyGenerator(Curve.P_256).keyID(VERIFIER_DID + "#verifier-key1").generate();

        trustedIssuerRegistry.register(new Issuer("did:web:localhost%%3A%s:issuer".formatted(CALLBACK_PORT), Map.of()), WILDCARD);

        var didDocumentJson = createDidDocumentJson();

        server.stubFor(get("/verifier/did.json")
                .willReturn(okJson(didDocumentJson)));


        when(STS_MOCK.createToken(any(), anyMap(), isNull()))
                .thenAnswer(i -> {
                    Map<String, Object> claims = i.getArgument(1);

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

    @DisplayName("Run TCK Presentation Flow tests")
    @Test
    void runPresentationFlowTests() {
        var monitor = new ConsoleMonitor(true, true);

        var triggerPath = PROTOCOL_API_PATH + "/2025-1/catalog/request";
        var holderDid = "did:web:localhost%3A" + CALLBACK_PORT + ":holder";
        var thirdPartyDid = "did:web:localhost%3A" + CALLBACK_PORT + ":thirdparty";
        var baseCallbackUrl = "http://localhost:%s".formatted(CALLBACK_PORT);
        var baseCallbackUri = URI.create(baseCallbackUrl);
        var result = TckRuntime.Builder.newInstance()
                .properties(Map.of(
                        "dataspacetck.callback.address", baseCallbackUrl,
                        "dataspacetck.host", baseCallbackUri.getHost(),
                        "dataspacetck.port", String.valueOf(baseCallbackUri.getPort()),
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

        assertThat(result.getFailures()).withFailMessage(() -> result.getFailures().stream()
                .map(f -> "- " + f.getTestIdentifier().getDisplayName() + " (" + f.getException() + ")")
                .collect(Collectors.joining("\n"))).isEmpty();
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

}
