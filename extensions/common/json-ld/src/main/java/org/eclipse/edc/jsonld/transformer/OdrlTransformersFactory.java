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

package org.eclipse.edc.jsonld.transformer;

import org.eclipse.edc.jsonld.transformer.to.JsonObjectToActionTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToConstraintTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDutyTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToOperatorTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToPermissionTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToPolicyTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToProhibitionTransformer;
import org.eclipse.edc.transform.spi.TypeTransformer;

import java.util.stream.Stream;

public final class OdrlTransformersFactory {

    private OdrlTransformersFactory() {
    }

    public static Stream<TypeTransformer<?, ?>> jsonObjectToOdrlTransformers() {
        return Stream.of(
                new JsonObjectToPolicyTransformer(),
                new JsonObjectToPermissionTransformer(),
                new JsonObjectToProhibitionTransformer(),
                new JsonObjectToDutyTransformer(),
                new JsonObjectToActionTransformer(),
                new JsonObjectToConstraintTransformer(),
                new JsonObjectToOperatorTransformer()
        );
    }
}
