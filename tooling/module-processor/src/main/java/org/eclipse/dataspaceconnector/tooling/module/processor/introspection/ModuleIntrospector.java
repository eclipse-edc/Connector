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

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.EdcSettingContext;
import org.eclipse.dataspaceconnector.spi.system.Extension;
import org.eclipse.dataspaceconnector.spi.system.ExtensionPoint;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Spi;
import org.eclipse.dataspaceconnector.tooling.module.domain.ConfigurationSetting;
import org.eclipse.dataspaceconnector.tooling.module.domain.ModuleType;
import org.eclipse.dataspaceconnector.tooling.module.domain.Service;
import org.eclipse.dataspaceconnector.tooling.module.domain.ServiceReference;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import static java.util.stream.Collectors.toList;
import static org.eclipse.dataspaceconnector.tooling.module.processor.compiler.AnnotationFunctions.attributeStringValues;
import static org.eclipse.dataspaceconnector.tooling.module.processor.compiler.AnnotationFunctions.attributeTypeValues;
import static org.eclipse.dataspaceconnector.tooling.module.processor.compiler.AnnotationFunctions.attributeValue;
import static org.eclipse.dataspaceconnector.tooling.module.processor.compiler.AnnotationFunctions.mirrorFor;
import static org.eclipse.dataspaceconnector.tooling.module.processor.compiler.ElementFunctions.typeFor;

/**
 * Contains methods for introspecting the current module using the Java Compiler API.
 */
public class ModuleIntrospector {
    private Elements elementUtils;

    public ModuleIntrospector(Elements elementUtils) {
        this.elementUtils = elementUtils;
    }

    /**
     * Returns the module name set by either the {@link Spi} or {@link Extension} annotation.
     */
    public String getModuleName(ModuleType moduleType, RoundEnvironment environment) {
        if (ModuleType.EXTENSION == moduleType) {
            var extensionElement = environment.getElementsAnnotatedWith(Extension.class).iterator().next();
            return attributeValue(String.class, "value", mirrorFor(Extension.class, extensionElement), elementUtils);
        } else {
            var extensionElement = environment.getElementsAnnotatedWith(Spi.class).iterator().next();
            return attributeValue(String.class, "value", mirrorFor(Spi.class, extensionElement), elementUtils);
        }
    }

    /**
     * Returns module categories set using either the {@link Spi} or {@link Extension} annotation.
     */
    public List<String> getModuleCategories(ModuleType moduleType, RoundEnvironment environment) {
        if (ModuleType.EXTENSION == moduleType) {
            var extensionElement = environment.getElementsAnnotatedWith(Extension.class).iterator().next();
            return attributeStringValues("categories", mirrorFor(Extension.class, extensionElement), elementUtils);
        } else {
            var extensionElement = environment.getElementsAnnotatedWith(Spi.class).iterator().next();
            return attributeStringValues("categories", mirrorFor(Spi.class, extensionElement), elementUtils);
        }
    }

    /**
     * Resolves referenced services by introspecting usages of {@link Inject}.
     */
    public List<ServiceReference> resolveReferencedServices(RoundEnvironment environment) {
        return environment.getElementsAnnotatedWith(Inject.class).stream()
                .map(element -> {
                    var required = attributeValue(Boolean.class, "required", mirrorFor(Inject.class, element), elementUtils);
                    return new ServiceReference(typeFor(element), required);
                })
                .collect(toList());
    }

    /**
     * Resolves referenced services by introspecting the {@link Provides} annotation.
     */
    public List<Service> resolveProvidedServices(RoundEnvironment environment) {
        return environment.getElementsAnnotatedWith(Provides.class).stream()
                .flatMap(element -> attributeTypeValues("value", mirrorFor(Provides.class, element), elementUtils).stream())
                .map(Service::new)
                .collect(toList());
    }

    /**
     * Resolves extension points declared with {@link ExtensionPoint}.
     */
    public List<Service> resolveExtensionPoints(RoundEnvironment environment) {
        return environment.getElementsAnnotatedWith(ExtensionPoint.class).stream()
                .map(element -> new Service(element.asType().toString()))
                .collect(toList());
    }

    /**
     * Resolves configuration points declared with {@link EdcSetting}.
     */
    public List<ConfigurationSetting> resolveConfigurationSettings(RoundEnvironment environment) {
        return environment.getElementsAnnotatedWith(EdcSetting.class).stream()
                .filter(VariableElement.class::isInstance)
                .map(VariableElement.class::cast)
                .map(this::createConfigurationSetting)
                .collect(toList());
    }

    /**
     * Maps a {@link ConfigurationSetting} from an {@link EdcSetting} annotation.
     */
    private ConfigurationSetting createConfigurationSetting(VariableElement settingElement) {
        var prefix = resolveConfigurationPrefix(settingElement);
        var keyValue = prefix + settingElement.getConstantValue().toString();

        var settingBuilder = ConfigurationSetting.Builder.newInstance().key(keyValue);

        var settingMirror = mirrorFor(EdcSetting.class, settingElement);

        var description = attributeValue(String.class, "value", settingMirror, elementUtils);
        settingBuilder.description(description);

        var type = attributeValue(String.class, "type", settingMirror, elementUtils);
        settingBuilder.type(type);

        var required = attributeValue(Boolean.class, "required", settingMirror, elementUtils);
        settingBuilder.required(required);

        var max = attributeValue(Long.class, "max", settingMirror, elementUtils);
        settingBuilder.maximum(max);

        var min = attributeValue(Long.class, "min", settingMirror, elementUtils);
        settingBuilder.minimum(min);

        return settingBuilder.build();
    }

    /**
     * Resolves a configuration prefix specified by {@link EdcSettingContext} for a given EDC setting element or an empty string if there is none.
     */
    @NotNull
    private String resolveConfigurationPrefix(VariableElement edcSettingElement) {
        var enclosingElement = edcSettingElement.getEnclosingElement();
        if (enclosingElement == null) {
            return "";
        }
        var contextMirror = mirrorFor(EdcSettingContext.class, enclosingElement);
        return contextMirror != null ? attributeValue(String.class, "value", contextMirror, elementUtils) : "";
    }


}
