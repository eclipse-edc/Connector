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

package org.eclipse.dataspaceconnector.tooling.module.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Spi;
import org.eclipse.dataspaceconnector.runtime.metamodel.domain.EdcModule;
import org.eclipse.dataspaceconnector.runtime.metamodel.domain.ModuleType;
import org.eclipse.dataspaceconnector.tooling.module.processor.introspection.ModuleIntrospector;
import org.eclipse.dataspaceconnector.tooling.module.processor.introspection.OverviewIntrospector;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

/**
 * Generates an EDC module manifest by introspecting a set of bounded artifacts.
 * <p>
 * Two processor parameters must be set: {@link #ID} which by convention uses Maven group id an artifact id coordinates;
 * and {@link #VERSION}. To Override the location where the manifest is generated, specify
 * {@link #EDC_LOCATION_OVERRIDE} as a processor parameter.
 */
@SupportedAnnotationTypes({
        "org.eclipse.dataspaceconnector.spi.EdcSetting",
        "org.eclipse.dataspaceconnector.spi.EdcSettingContext",
        "org.eclipse.dataspaceconnector.spi.system.Extension",
        "org.eclipse.dataspaceconnector.spi.system.Spi",
        "org.eclipse.dataspaceconnector.spi.system.ExtensionPoint",
        "org.eclipse.dataspaceconnector.spi.system.Provider",
        "org.eclipse.dataspaceconnector.spi.system.Provides",
        "org.eclipse.dataspaceconnector.spi.system.Requires",
        "org.eclipse.dataspaceconnector.spi.system.Inject",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedOptions({ EdcModuleProcessor.ID, EdcModuleProcessor.VERSION })
public class EdcModuleProcessor extends AbstractProcessor {
    static final String VERSION = "edc.version";
    static final String ID = "edc.id";

    private static final String MANIFEST_NAME = "edc.json";
    private static final String EDC_LOCATION_OVERRIDE = "edc.location";

    private final ObjectMapper mapper = new ObjectMapper();

    private ModuleIntrospector moduleIntrospector;

    private OverviewIntrospector overviewIntrospector;

    private EdcModule.Builder moduleBuilder;

    private ModuleType moduleType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        moduleIntrospector = new ModuleIntrospector(processingEnv.getElementUtils());
        //todo: replace this Noop converter with an actual JavadocConverter
        overviewIntrospector = new OverviewIntrospector(javadoc -> javadoc, processingEnv.getElementUtils());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        if (!initializeModuleBuilder(environment)) {
            return false;  // error, do not continue processing
        }

        if (environment.processingOver()) {
            if (moduleBuilder != null) {
                writeManifest();
            }
            return false; // processing rounds are complete, return
        }

        moduleBuilder.provides(moduleIntrospector.resolveProvidedServices(environment));

        moduleBuilder.extensionPoints(moduleIntrospector.resolveExtensionPoints(environment));

        moduleBuilder.references(moduleIntrospector.resolveReferencedServices(environment));

        moduleBuilder.configuration(moduleIntrospector.resolveConfigurationSettings(environment));

        moduleBuilder.overview(overviewIntrospector.generateModuleOverview(moduleType, environment));

        return false;
    }

    private boolean initializeModuleBuilder(RoundEnvironment environment) {
        if (moduleBuilder != null) {
            // already initialized in a previous round
            return true;
        }

        var id = processingEnv.getOptions().get(ID);
        if (id == null) {
            processingEnv.getMessager().printMessage(ERROR, "Value for '" + ID + "' not set on processor configuration. Skipping manifest generation.");
            return false;
        }

        var version = processingEnv.getOptions().get(VERSION);
        if (version == null) {
            processingEnv.getMessager().printMessage(ERROR, "Value for '" + VERSION + "' not set on processor configuration. Skipping manifest generation.");
            return false;
        }

        moduleType = determineAndValidateModuleType(environment);
        if (moduleType == null) {
            // error or not a module, return
            return false;
        }

        var name = moduleIntrospector.getModuleName(moduleType, environment);

        var categories = moduleIntrospector.getModuleCategories(moduleType, environment);

        moduleBuilder = EdcModule.Builder.newInstance().id(id).version(version).type(moduleType).name(name).categories(categories);

        return true;
    }

    @Nullable
    private ModuleType determineAndValidateModuleType(RoundEnvironment environment) {
        var extensionElements = environment.getElementsAnnotatedWith(Extension.class);
        if (extensionElements.isEmpty()) {
            // check if it is an SPI
            var spiElements = environment.getElementsAnnotatedWith(Spi.class);
            if (spiElements.size() > 1) {
                var types = spiElements.stream().map(e -> e.asType().toString()).collect(Collectors.joining(", "));
                processingEnv.getMessager().printMessage(ERROR, "Multiple SPI definitions found in module: " + types);
                return null;
            } else if (spiElements.isEmpty()) {
                processingEnv.getMessager().printMessage(NOTE, "Note an EDC module. Skipping module processing.");
                return null;
            }
            return ModuleType.SPI;

        } else {
            // an extension
            if (extensionElements.size() > 1) {
                var types = extensionElements.stream().map(e -> e.asType().toString()).collect(Collectors.joining(", "));
                processingEnv.getMessager().printMessage(ERROR, "Multiple extension types found in module: " + types);
                return null;
            }
            return ModuleType.EXTENSION;
        }
    }

    private void writeManifest() {
        try {
            var filer = processingEnv.getFiler();
            var location = processingEnv.getOptions().get(EDC_LOCATION_OVERRIDE);
            if (location != null) {
                new File(location).mkdirs();
                try (var writer = new BufferedWriter(new FileWriter(location + File.separator + MANIFEST_NAME))) {
                    mapper.writeValue(writer, List.of(moduleBuilder.build()));
                }
            } else {
                var resource = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", MANIFEST_NAME);
                try (var writer = resource.openWriter()) {
                    mapper.writeValue(writer, List.of(moduleBuilder.build()));
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


}
