/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.tooling.module.processor.introspection;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Spi;
import org.eclipse.dataspaceconnector.runtime.metamodel.domain.ModuleType;
import org.eclipse.dataspaceconnector.tooling.module.processor.generator.JavadocConverter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.util.Elements;

/**
 * Generates a module overview.
 */
public class OverviewIntrospector {
    private final JavadocConverter javadocConverter;
    private final Elements elementUtils;

    public OverviewIntrospector(@Nullable JavadocConverter javadocConverter, Elements elementUtils) {
        this.javadocConverter = javadocConverter;
        this.elementUtils = elementUtils;
    }

    /**
     * Generated overview documentation by converting Javadoc to a Markdown representation. For SPI modules, the Javadoc is taken from the <code>package-info.java</code> type
     * annotated with {@link Spi}. For extensions, the Javadoc is taken from the type annotated with {@link Extension}.
     */
    @Nullable
    public String generateModuleOverview(ModuleType moduleType, RoundEnvironment environment) {
        var annotation = moduleType == ModuleType.EXTENSION ? Extension.class : Spi.class;
        var elements = environment.getElementsAnnotatedWith(annotation);
        if (elements.isEmpty()) {
            return null;
        }

        var moduleElement = elements.iterator().next();
        var javadoc = elementUtils.getDocComment(moduleElement);

        if (javadoc == null) {
            return "No overview provided.";
        }
        return javadocConverter != null ? javadocConverter.generate(javadoc) : "No JavadocConverter available";
    }


}
