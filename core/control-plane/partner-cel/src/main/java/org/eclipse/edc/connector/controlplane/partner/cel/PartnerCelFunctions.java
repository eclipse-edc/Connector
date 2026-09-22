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
import org.eclipse.edc.policy.cel.function.CelFunction;
import org.eclipse.edc.policy.cel.function.CelValueType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.policy.cel.function.CelValueType.BOOL;
import static org.eclipse.edc.policy.cel.function.CelValueType.LIST;
import static org.eclipse.edc.policy.cel.function.CelValueType.MAP;
import static org.eclipse.edc.policy.cel.function.CelValueType.STRING;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * CEL helper functions over partners. The receiver of the lookup functions is the {@code ctx.partners} handle, which
 * carries the id of the participant context the policy is evaluated for:
 * <pre>
 * ctx.partners.byIdentity(ctx.agent.id).inGroup('gold')
 * ctx.partners.byId('acme').found()
 * ctx.partners.query({'businessId': 'BID-0001'}).size() > 0
 * 'gold' in ctx.partners.byIdentity(ctx.agent.id).groups
 * </pre>
 * A partner is exposed as a map {@code {id, identity, name, properties, groups, found}}. Lookups never return
 * {@code null}: an unknown partner is a map with {@code found == false} and empty groups, so membership checks on
 * unknown counterparties evaluate to {@code false} instead of aborting the evaluation.
 */
public class PartnerCelFunctions {

    public static final String PARTICIPANT_CONTEXT_ID = "participantContextId";
    public static final String ID = "id";
    public static final String IDENTITY = "identity";
    public static final String NAME = "name";
    public static final String PROPERTIES = "properties";
    public static final String GROUPS = "groups";
    public static final String FOUND = "found";

    /**
     * Upper bound for {@code query} results, so an evaluation never loads an unbounded number of partners.
     */
    public static final int MAX_QUERY_RESULTS = 1000;

    private PartnerCelFunctions() {
    }

    public static List<CelFunction> functions(PartnerService partnerService) {
        return List.of(
                // --- ctx.partners handle receiver ------------------------------------------------------------
                fn("byIdentity", "partner_handle_by_identity", MAP, List.of(MAP, STRING),
                        args -> byIdentity(partnerService, args.get(0), string(args.get(1)))),
                fn("byId", "partner_handle_by_id", MAP, List.of(MAP, STRING),
                        args -> byId(partnerService, args.get(0), string(args.get(1)))),
                fn("query", "partner_handle_query", LIST, List.of(MAP, MAP),
                        args -> query(partnerService, args.get(0), asMap(args.get(1)))),

                // --- partner receiver --------------------------------------------------------------------------
                fn("inGroup", "partner_in_group", BOOL, List.of(MAP, STRING),
                        args -> groups(args.get(0)).contains(string(args.get(1)))),
                fn("groups", "partner_groups", LIST, List.of(MAP),
                        args -> groups(args.get(0))),
                fn("found", "partner_found", BOOL, List.of(MAP),
                        args -> Boolean.TRUE.equals(asMap(args.get(0)).get(FOUND)))
        );
    }

    /**
     * Converts a partner into its CEL representation.
     */
    public static Map<String, Object> toMap(Partner partner) {
        var map = new HashMap<String, Object>();
        map.put(ID, partner.getId());
        map.put(IDENTITY, partner.getIdentity());
        map.put(NAME, partner.getName() == null ? "" : partner.getName());
        map.put(PROPERTIES, withoutNulls(partner.getProperties()));
        map.put(GROUPS, new ArrayList<>(partner.getGroupIds()));
        map.put(FOUND, true);
        return map;
    }

    /**
     * The representation of a partner that does not exist.
     */
    public static Map<String, Object> notFound(String identity) {
        var map = new HashMap<String, Object>();
        map.put(ID, "");
        map.put(IDENTITY, identity == null ? "" : identity);
        map.put(NAME, "");
        map.put(PROPERTIES, Map.of());
        map.put(GROUPS, List.of());
        map.put(FOUND, false);
        return map;
    }

    private static Map<String, Object> byIdentity(PartnerService service, Object handle, String identity) {
        var participantContextId = participantContextId(handle);
        if (participantContextId == null) {
            return notFound(identity);
        }
        return service.findByIdentity(participantContextId, identity)
                .map(PartnerCelFunctions::toMap)
                .orElse(f -> notFound(identity));
    }

    private static Map<String, Object> byId(PartnerService service, Object handle, String id) {
        var participantContextId = participantContextId(handle);
        if (participantContextId == null) {
            return notFound("");
        }
        return service.findById(participantContextId, id)
                .map(PartnerCelFunctions::toMap)
                .orElse(f -> notFound(""));
    }

    private static List<Map<String, Object>> query(PartnerService service, Object handle, Map<String, Object> filters) {
        var participantContextId = participantContextId(handle);
        if (participantContextId == null) {
            return List.of();
        }
        var builder = queryByParticipantContextId(participantContextId).limit(MAX_QUERY_RESULTS);
        filters.forEach((key, value) -> builder.filter(criterion(PROPERTIES + "." + key, "=", value)));
        return service.search(builder.build())
                .map(partners -> partners.stream().map(PartnerCelFunctions::toMap).toList())
                .orElse(f -> List.of());
    }

    private static String participantContextId(Object handle) {
        var value = asMap(handle).get(PARTICIPANT_CONTEXT_ID);
        return value == null ? null : value.toString();
    }

    private static List<String> groups(Object partner) {
        var groups = asMap(partner).get(GROUPS);
        if (groups instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private static Map<String, Object> withoutNulls(Map<String, Object> properties) {
        var copy = new HashMap<String, Object>();
        properties.forEach((key, value) -> {
            if (value != null) {
                copy.put(key, value);
            }
        });
        return copy;
    }

    private static CelFunction fn(String name, String overloadId, CelValueType resultType,
                                  List<CelValueType> argumentTypes, Function<List<Object>, Object> implementation) {
        return new CelFunction(name, overloadId, true, resultType, argumentTypes, implementation);
    }
}
