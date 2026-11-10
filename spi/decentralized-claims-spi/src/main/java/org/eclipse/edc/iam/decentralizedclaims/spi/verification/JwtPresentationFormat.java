/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.spi.verification;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX_V2;

/**
 * Determines the format of a presentation that is represented as JWT:
 * <ul>
 *     <li>{@link CredentialFormat#VC1_0_JWT}: the presentation is in the {@code vp} claim, and the JWT has no VCDM 2.0 {@code @context}</li>
 *     <li>{@link CredentialFormat#VC2_0_JOSE}: the presentation is the JWT payload, which has the VCDM 2.0 {@code @context} and no {@code vp} claim</li>
 * </ul>
 * Any other JWT, e.g. one with a {@code vc} claim, is not a valid presentation.
 */
public final class JwtPresentationFormat {

    private static final String VP_CLAIM = "vp";
    private static final String VC_CLAIM = "vc";
    private static final String CONTEXT_CLAIM = "@context";

    private JwtPresentationFormat() {
    }

    /**
     * Returns the format of the presentation JWT.
     *
     * @param claims the claims of the JWT
     * @return {@link CredentialFormat#VC1_0_JWT}, {@link CredentialFormat#VC2_0_JOSE}, or null if the JWT is not a presentation of exactly one of them
     */
    public static @Nullable CredentialFormat of(Map<String, Object> claims) {
        if (claims.containsKey(VC_CLAIM)) {
            return null;
        }
        var hasVpClaim = claims.containsKey(VP_CLAIM);
        var hasVcdm20Context = hasVcdm20Context(claims);
        if (hasVpClaim) {
            return hasVcdm20Context ? null : CredentialFormat.VC1_0_JWT;
        }
        return hasVcdm20Context ? CredentialFormat.VC2_0_JOSE : null;
    }

    private static boolean hasVcdm20Context(Map<String, Object> claims) {
        var context = claims.get(CONTEXT_CLAIM);
        if (context instanceof Collection<?> contexts) {
            return contexts.contains(VC_PREFIX_V2);
        }
        return VC_PREFIX_V2.equals(context);
    }
}
