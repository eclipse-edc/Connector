/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;

import java.util.Map;
import java.util.function.Consumer;

abstract class AbstractAssetTransformer {

    protected <T> void extractProperty(TransformerContext context, Map<String, Object> properties, String propertyKey, Class<T> targetType, Consumer<T> consumer) {
        var propertyValue = properties.get(propertyKey);
        if (propertyValue == null) {
            context.reportProblem(String.format("Asset property %s is null", propertyKey));
        } else {
            if (targetType.isAssignableFrom(propertyValue.getClass())) {
                consumer.accept(targetType.cast(propertyValue));
            } else {
                T convertedPropertyValue;
                if ((convertedPropertyValue = context.transform(propertyValue, targetType)) != null) {
                    consumer.accept(convertedPropertyValue);
                } else {
                    context.reportProblem(String.format("Asset property %s not convertible to %s", propertyKey, targetType.getSimpleName()));
                }
            }
        }
    }
}
