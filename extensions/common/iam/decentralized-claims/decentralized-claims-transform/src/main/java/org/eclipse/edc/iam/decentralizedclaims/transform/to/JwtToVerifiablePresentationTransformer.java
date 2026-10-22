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
 *       Cofinity-X - updates for VCDM 2.0
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.transform.to;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;

import java.text.ParseException;
import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VERIFIABLE_PRESENTATION_TYPE;


@SuppressWarnings("unchecked")
public class JwtToVerifiablePresentationTransformer extends AbstractJwtTransformer<VerifiablePresentation> {

    private static final String VERIFIABLE_CREDENTIAL_PROPERTY = "verifiableCredential";
    private static final String VERIFIABLE_CREDENTIAL_PROPERTY_ID = "id";
    private static final String VP_CLAIM = "vp";
    private static final String DATA_URL_VP_JWT = "data:application/vp+jwt,";
    private static final String DATA_URL_VC_JWT = "data:application/vc+jwt,";
    private static final String ENVELOPED_PRESENTATION_TYPE = "EnvelopedVerifiablePresentation";

    private final Monitor monitor;
    private final TypeManager typeManager;
    private final String typeContext;
    private final JsonLd jsonLd;

    public JwtToVerifiablePresentationTransformer(Monitor monitor, TypeManager typeManager, String typeContext, JsonLd jsonLd) {
        super(VerifiablePresentation.class);
        this.monitor = monitor;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.jsonLd = jsonLd;
    }

    @Override
    public @Nullable VerifiablePresentation transform(@NotNull String jsonWebToken, @NotNull TransformerContext context) {
        try {
            var builder = VerifiablePresentation.Builder.newInstance();
            var signedJwt = SignedJWT.parse(jsonWebToken);
            var claimsSet = signedJwt.getJWTClaimsSet();
            if (isVcDataModel2_0(claimsSet)) {
                return parseVcdm20Presentation(claimsSet, builder, context);
            } else {

                var vpObject = claimsSet.getClaim(VP_CLAIM);
                builder.holder(claimsSet.getIssuer());
                builder.id(claimsSet.getJWTID());

                if (vpObject instanceof String) {
                    vpObject = typeManager.getMapper(typeContext).readValue(vpObject.toString(), Map.class);
                }

                if (vpObject instanceof Map<?, ?> vp) {
                    // types
                    listOrReturn(vp.get(TYPE_PROPERTY), Object::toString).forEach(builder::type);

                    // verifiable credentials
                    listOrReturn(vp.get(VERIFIABLE_CREDENTIAL_PROPERTY), o -> extractCredentials(o, context)).forEach(builder::credential);

                    return builder.build();
                }
            }


        } catch (ParseException | JacksonException e) {
            monitor.warning("Error parsing JWT", e);
            context.reportProblem("Error parsing JWT: %s".formatted(e.getMessage()));
        }
        context.reportProblem("Could not parse VerifiablePresentation from JWT.");
        return null;
    }

    /**
     * Parses a VCDM 2.0 presentation, which is either the VP JWT itself (type {@code VerifiablePresentation}) or a JWT of type
     * {@code EnvelopedVerifiablePresentation} that carries the VP JWT in its {@code id} claim.
     */
    private @Nullable VerifiablePresentation parseVcdm20Presentation(JWTClaimsSet claimsSet, VerifiablePresentation.Builder builder, @NotNull TransformerContext context) throws ParseException {
        var types = listOrReturn(claimsSet.getClaim(TYPE_PROPERTY), Object::toString);
        if (types.contains(ENVELOPED_PRESENTATION_TYPE)) {
            // the "id" claim must be present, and start with "data:application/vp+jwt"
            var idClaim = claimsSet.getStringClaim(VERIFIABLE_CREDENTIAL_PROPERTY_ID);
            if (idClaim != null && idClaim.startsWith(DATA_URL_VP_JWT)) {
                var vpToken = idClaim.substring(DATA_URL_VP_JWT.length());
                return parseVpClaims(SignedJWT.parse(vpToken).getJWTClaimsSet(), builder, context);
            } else {
                context.reportProblem("EnvelopedVerifiablePresentation objects must have their VP token encoded in the 'id' property: \"id\":\"data:application/vp+jwt,<VPTOKEN>\"");
                return null;
            }
        }
        if (types.contains(VERIFIABLE_PRESENTATION_TYPE)) {
            return parseVpClaims(claimsSet, builder, context);
        }
        context.reportProblem("Presentations secured with JOSE MUST be of type '%s' or '%s', but was %s".formatted(VERIFIABLE_PRESENTATION_TYPE, ENVELOPED_PRESENTATION_TYPE, types));
        return null;
    }

    private VerifiablePresentation parseVpClaims(JWTClaimsSet vpClaims, VerifiablePresentation.Builder builder, @NotNull TransformerContext context) throws ParseException {
        builder.holder(vpClaims.getStringClaim("holder"));
        builder.id(vpClaims.getJWTID());
        // verifiable credentials as EnvelopedVerifiableCredentials
        listOrReturn(vpClaims.getClaim(VERIFIABLE_CREDENTIAL_PROPERTY), o -> extractEnvelopedCredential(o, context)).forEach(builder::credential);
        // types
        listOrReturn(vpClaims.getClaim(TYPE_PROPERTY), Object::toString).forEach(builder::type);

        return builder.dataModelVersion(DataModelVersion.V_2_0).build();
    }

    private VerifiableCredential extractEnvelopedCredential(Object credentialObject, @NotNull TransformerContext context) {
        if (credentialObject instanceof Map<?, ?> credential) {
            var type = credential.get(TYPE_PROPERTY).toString();
            var id = credential.get(VERIFIABLE_CREDENTIAL_PROPERTY_ID).toString();

            if (!"EnvelopedVerifiableCredential".equals(type)) {
                context.reportProblem(String.format("'type' must be an EnvelopedVerifiableCredential, but was '%s'", type));
                return null;
            }

            if (!id.startsWith(DATA_URL_VC_JWT)) {
                context.reportProblem("EnvelopedVerifiableCredential objects must have their VC token encoded in the 'id' property: \"id\":\"data:application/vc+jwt,<VCTOKEN>\"");
                return null;
            }

            var vcToken = id.substring(DATA_URL_VC_JWT.length());
            return extractCredentials(vcToken, context);

        }
        context.reportProblem("Credential object is not a valid EnvelopedVerifiableCredential");
        return null;
    }


    @Nullable
    private VerifiableCredential extractCredentials(Object credential, TransformerContext context) {
        if (credential instanceof String) { // VC is JWT
            return context.transform(credential.toString(), VerifiableCredential.class);
        }
        // VC is LDP
        var input = typeManager.getMapper(typeContext).convertValue(credential, JsonObject.class);
        var expansion = jsonLd.expand(input);
        if (expansion.succeeded()) {
            return context.transform(expansion.getContent(), VerifiableCredential.class);
        }
        context.reportProblem("Error expanding embedded VC: %s".formatted(expansion.getFailureDetail()));
        return null;

    }
}
