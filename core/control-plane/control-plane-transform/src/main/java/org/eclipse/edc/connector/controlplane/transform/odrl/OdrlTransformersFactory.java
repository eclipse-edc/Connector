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

package org.eclipse.edc.connector.controlplane.transform.odrl;

import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToActionTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToConstraintTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToDutyTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToOperatorTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToPermissionTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToPolicyTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.to.JsonObjectToProhibitionTransformer;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.transform.spi.TypeTransformer;

import java.util.stream.Stream;

public final class OdrlTransformersFactory {

    private OdrlTransformersFactory() {
    }

    public static Stream<TypeTransformer<?, ?>> jsonObjectToOdrlTransformers(ParticipantIdMapper participantIdMapper) {
        return jsonObjectToOdrlTransformers(participantIdMapper, false);
    }

    /**
     * Creates the JSON-LD to ODRL transformers.
     *
     * @param participantIdMapper the participant id mapper.
     * @param strictRightOperand  if true, a constraint right operand that is not a literal or an array of literals is
     *                            rejected, e.g. a node reference ({@code {"@id": X}}) is not read as the string X.
     * @return the transformers.
     */
    public static Stream<TypeTransformer<?, ?>> jsonObjectToOdrlTransformers(ParticipantIdMapper participantIdMapper, boolean strictRightOperand) {
        return Stream.of(
                new JsonObjectToPolicyTransformer(participantIdMapper),
                new JsonObjectToPermissionTransformer(),
                new JsonObjectToProhibitionTransformer(),
                new JsonObjectToDutyTransformer(),
                new JsonObjectToActionTransformer(),
                new JsonObjectToConstraintTransformer(strictRightOperand),
                new JsonObjectToOperatorTransformer()
        );
    }
}
