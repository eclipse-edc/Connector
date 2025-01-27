/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.catalog.transform.v2024.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DSPACE_PROPERTY_PARTICIPANT_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;

/**
 * Converts from a {@link Catalog} to a DCAT catalog as a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromCatalogV2024Transformer extends AbstractNamespaceAwareJsonLdTransformer<Catalog, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;
    private final ParticipantIdMapper participantIdMapper;

    public JsonObjectFromCatalogV2024Transformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext, ParticipantIdMapper participantIdMapper) {
        this(jsonFactory, typeManager, typeContext, participantIdMapper, DSP_NAMESPACE_V_2024_1);
    }

    public JsonObjectFromCatalogV2024Transformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext, ParticipantIdMapper participantIdMapper, JsonLdNamespace namespace) {
        super(Catalog.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.participantIdMapper = participantIdMapper;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Catalog catalog, @NotNull TransformerContext context) {
        var partitions = catalog.getDatasets().stream().collect(Collectors.groupingBy(Dataset::getClass));

        var datasets = ofNullable(partitions.get(Dataset.class)).orElseGet(ArrayList::new)
                .stream()
                .map(offer -> context.transform(offer, JsonObject.class))
                .collect(toJsonArray());

        var subCatalogs = ofNullable(partitions.get(Catalog.class)).orElseGet(ArrayList::new)
                .stream()
                .map(offer -> context.transform(offer, JsonObject.class))
                .collect(toJsonArray());

        var dataServices = catalog.getDataServices().stream()
                .map(service -> context.transform(service, JsonObject.class))
                .collect(toJsonArray());

        var distributions = catalog.getDistributions().stream()
                .map(distro -> context.transform(distro, JsonObject.class))
                .collect(toJsonArray());

        var objectBuilder = jsonFactory.createObjectBuilder()
                .add(ID, catalog.getId())
                .add(TYPE, DCAT_CATALOG_TYPE)
                .add(DCAT_DATASET_ATTRIBUTE, datasets)
                .add(DCAT_CATALOG_ATTRIBUTE, subCatalogs)
                .add(DCAT_DISTRIBUTION_ATTRIBUTE, distributions)
                .add(DCAT_DATA_SERVICE_ATTRIBUTE, dataServices);

        ofNullable(catalog.getParticipantId()).ifPresent(pid -> objectBuilder.add(forNamespace(DSPACE_PROPERTY_PARTICIPANT_ID_TERM), createId(jsonFactory, participantIdMapper.toIri(pid))));

        transformProperties(catalog.getProperties(), objectBuilder, typeManager.getMapper(typeContext), context);

        return objectBuilder.build();
    }

}
