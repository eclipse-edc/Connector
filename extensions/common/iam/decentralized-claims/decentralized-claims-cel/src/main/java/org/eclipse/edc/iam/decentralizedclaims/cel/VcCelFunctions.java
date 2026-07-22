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

import org.eclipse.edc.policy.cel.function.CelFunction;
import org.eclipse.edc.policy.cel.function.CelValueType;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.eclipse.edc.policy.cel.function.CelValueType.BOOL;
import static org.eclipse.edc.policy.cel.function.CelValueType.DYN;
import static org.eclipse.edc.policy.cel.function.CelValueType.LIST;
import static org.eclipse.edc.policy.cel.function.CelValueType.MAP;
import static org.eclipse.edc.policy.cel.function.CelValueType.STRING;

/**
 * Helper functions for querying verifiable credentials from CEL expressions, so that
 * <pre>
 * ctx.agent.claims.vc.filter(c, c.type.exists(t, t == 'MembershipCredential'))
 *                    .exists(c, c.credentialSubject.exists(cs, cs.memberOf == 'Catena-X'))
 * </pre>
 * can be written as
 * <pre>
 * ctx.agent.claims.vc.withType('MembershipCredential').hasClaim('memberOf', 'Catena-X')
 * </pre>
 * <p>
 * All functions operate on the credential representation produced by {@link VcClaimMapper} and are shape-safe: a
 * missing key, a wrongly typed value or a non-credential entry yields "no match" rather than an evaluation error.
 * This is a deliberate departure from plain CEL map access, where reading an absent key aborts evaluation.
 */
public class VcCelFunctions {

    private static final String TYPE = "type";
    private static final String CONTEXT = "@context";
    private static final String ISSUER = "issuer";
    private static final String ID = "id";
    private static final String CREDENTIAL_SUBJECT = "credentialSubject";
    private static final String ISSUANCE_DATE = "issuanceDate";
    private static final String EXPIRATION_DATE = "expirationDate";

    private VcCelFunctions() {
    }

    public static List<CelFunction> functions(Clock clock) {
        return List.of(
                // --- list receiver ---------------------------------------------------------------------------
                fn("withType", "vc_list_with_type", LIST, List.of(LIST, STRING),
                        args -> withType(args.get(0), string(args.get(1)))),
                fn("withContext", "vc_list_with_context", LIST, List.of(LIST, STRING),
                        args -> withContext(args.get(0), string(args.get(1)))),
                fn("withIssuer", "vc_list_with_issuer", LIST, List.of(LIST, STRING),
                        args -> withIssuer(args.get(0), string(args.get(1)))),
                fn("valid", "vc_list_valid", LIST, List.of(LIST),
                        args -> validOnly(args.get(0), clock.instant())),
                fn("hasCredential", "vc_list_has_credential", BOOL, List.of(LIST, STRING),
                        args -> anyCredential(args.get(0), c -> hasType(c, string(args.get(1))))),
                fn("hasClaim", "vc_list_has_claim_value", BOOL, List.of(LIST, STRING, DYN),
                        args -> anyClaim(args.get(0), string(args.get(1)), v -> Objects.equals(v, args.get(2)))),
                fn("hasClaim", "vc_list_has_claim", BOOL, List.of(LIST, STRING),
                        args -> anyClaim(args.get(0), string(args.get(1)), v -> true)),
                fn("claim", "vc_list_claim", DYN, List.of(LIST, STRING),
                        args -> firstClaim(args.get(0), string(args.get(1)))),
                fn("claims", "vc_list_claims", LIST, List.of(LIST, STRING),
                        args -> claimValues(args.get(0), string(args.get(1)))),

                // --- single credential receiver, so the helpers compose with the standard macros --------------
                fn("hasType", "vc_map_has_type", BOOL, List.of(MAP, STRING),
                        args -> hasType(args.get(0), string(args.get(1)))),
                fn("hasContext", "vc_map_has_context", BOOL, List.of(MAP, STRING),
                        args -> hasContext(args.get(0), string(args.get(1)))),
                fn("valid", "vc_map_valid", BOOL, List.of(MAP),
                        args -> isValid(args.get(0), clock.instant())),
                fn("hasClaim", "vc_map_has_claim_value", BOOL, List.of(MAP, STRING, DYN),
                        args -> anyClaim(List.of(args.get(0)), string(args.get(1)), v -> Objects.equals(v, args.get(2)))),
                fn("hasClaim", "vc_map_has_claim", BOOL, List.of(MAP, STRING),
                        args -> anyClaim(List.of(args.get(0)), string(args.get(1)), v -> true)),
                fn("claim", "vc_map_claim", DYN, List.of(MAP, STRING),
                        args -> firstClaim(List.of(args.get(0)), string(args.get(1))))
        );
    }

    private static CelFunction fn(String name, String overloadId, CelValueType resultType,
                                  List<CelValueType> argumentTypes, Function<List<Object>, Object> implementation) {
        return new CelFunction(name, overloadId, true, resultType, argumentTypes, implementation);
    }

    private static List<?> withType(Object credentials, String type) {
        return filter(credentials, c -> hasType(c, type));
    }

    private static List<?> withContext(Object credentials, String context) {
        return filter(credentials, c -> hasContext(c, context));
    }

    private static List<?> withIssuer(Object credentials, String issuer) {
        return filter(credentials, c -> Objects.equals(issuerId(c), issuer));
    }

    private static List<?> validOnly(Object credentials, Instant now) {
        return filter(credentials, c -> isValid(c, now));
    }

    private static List<?> filter(Object credentials, Predicate<Object> predicate) {
        return asList(credentials).stream().filter(predicate).toList();
    }

    private static boolean anyCredential(Object credentials, Predicate<Object> predicate) {
        return asList(credentials).stream().anyMatch(predicate);
    }

    private static boolean hasType(Object credential, String type) {
        return asList(asMap(credential).get(TYPE)).contains(type);
    }

    private static boolean hasContext(Object credential, String context) {
        return asList(asMap(credential).get(CONTEXT)).contains(context);
    }

    /**
     * The issuer is mapped as an object, but a custom claim mapper may emit the bare id, so both shapes are read.
     */
    private static String issuerId(Object credential) {
        var issuer = asMap(credential).get(ISSUER);
        if (issuer instanceof String s) {
            return s;
        }
        return asMap(issuer).get(ID) instanceof String s ? s : null;
    }

    /**
     * Mirrors {@code IsInValidityPeriod}, the rule applied by the credential verification pipeline: a credential is
     * valid once issued and until it expires, with the expiration instant itself still valid. A malformed date is
     * treated as invalid so this never widens access relative to that rule. Credentials arriving through the standard
     * DCP flow have already passed it, so this is defence in depth for other claim sources.
     */
    private static boolean isValid(Object credential, Instant now) {
        var credentialMap = asMap(credential);
        var issuance = instant(credentialMap.get(ISSUANCE_DATE));
        if (issuance == null || issuance.isAfter(now)) {
            return false;
        }
        var expiration = credentialMap.get(EXPIRATION_DATE);
        if (expiration == null) {
            return true;
        }
        var expirationInstant = instant(expiration);
        return expirationInstant != null && !expirationInstant.isBefore(now);
    }

    private static boolean anyClaim(Object credentials, String name, Predicate<Object> predicate) {
        var path = segments(name);
        for (var credential : asList(credentials)) {
            for (var subject : asList(asMap(credential).get(CREDENTIAL_SUBJECT))) {
                var value = resolve(subject, path);
                if (value != null && predicate.test(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Object firstClaim(Object credentials, String name) {
        var path = segments(name);
        for (var credential : asList(credentials)) {
            for (var subject : asList(asMap(credential).get(CREDENTIAL_SUBJECT))) {
                var value = resolve(subject, path);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Collects the values of a subject claim across all credentials.
     */
    private static List<Object> claimValues(Object credentials, String name) {
        var path = segments(name);
        var values = new ArrayList<>();
        for (var credential : asList(credentials)) {
            for (var subject : asList(asMap(credential).get(CREDENTIAL_SUBJECT))) {
                var value = resolve(subject, path);
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static String[] segments(String name) {
        return name == null ? new String[0] : name.split("\\.");
    }

    /**
     * Resolves a claim name against a subject. The whole name is tried as a literal key first, so keys that contain
     * dots — namespaced credential claims are usually IRIs — remain addressable; only then is it treated as a path
     * into nested claims.
     */
    private static Object resolve(Object subject, String[] path) {
        if (path.length == 0) {
            return null;
        }
        var subjectMap = asMap(subject);
        if (path.length == 1) {
            return subjectMap.get(path[0]);
        }
        var literal = subjectMap.get(String.join(".", path));
        if (literal != null) {
            return literal;
        }
        Object current = subject;
        for (var segment : path) {
            if (current == null) {
                return null;
            }
            current = asMap(current).get(segment);
        }
        return current;
    }

    private static Instant instant(Object value) {
        if (!(value instanceof String s)) {
            return null;
        }
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String string(Object value) {
        return value instanceof String s ? s : null;
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }
}
