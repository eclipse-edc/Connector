/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.transformer;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for transforming a Catalog (including Dataset, Distribution, DataService and Policy).
 */
@ExtendWith(DependencyInjectionExtension.class)
class CatalogTransformationTest {
    
    private static final String CATALOG_PROPERTY_KEY = "catalog:prop:key";
    private static final String DATASET_PROPERTY_KEY = "dataset:prop:key";
    
    private JsonLdTransformerRegistry transformerRegistry;
    private JsonDocument contextDocument;
    
    private Catalog catalog;
    private DataService dataService;
    private Dataset dataset;
    private Policy policy;
    private Distribution distribution;
    
    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TypeManager.class, mock(TypeManager.class));
        factory.constructInstance(JsonLdExtension.class).initialize(context);
        transformerRegistry = context.getService(JsonLdTransformerRegistry.class);
    
        // create context for compacting JSON-LD document
        var jsonFactory = Json.createBuilderFactory(Map.of());
        var contextObject = jsonFactory
                .createObjectBuilder()
                .add(Namespaces.DCAT_PREFIX, Namespaces.DCAT_SCHEMA)
                .add(Namespaces.ODRL_PREFIX, Namespaces.ODRL_SCHEMA)
                .add(Namespaces.DCT_PREFIX, Namespaces.DCT_SCHEMA)
                .build();
        contextDocument = JsonDocument.of(jsonFactory.createObjectBuilder()
                .add("@context", contextObject)
                .build());
    
        // create catalog
        policy = getPolicy();
        dataService = getDataService();
        distribution = getDistribution(dataService);
        dataset = getDataset(distribution, policy);
        catalog = getCatalog(dataset, dataService);
    }
    
    /**
     * Transforms the catalog to a JsonObject, uses the Titanium JSON-LD library to compact and
     * expand the JSON-LD, and then transforms it back to a catalog. Verifies that the
     * resulting catalog is equal to the initial one.
     *
     * @throws JsonLdError if compacting or expanding the document fails.
     */
    @Test
    void transformCatalogToJsonObjectToCatalog() throws JsonLdError {
        var toJsonResult = transformerRegistry.transform(catalog, JsonObject.class);
    
        assertThat(toJsonResult.succeeded()).isTrue();
        var catalogJson = toJsonResult.getContent();
        assertThat(catalogJson).isNotNull();
        
        var compacted = JsonLd.compact(JsonDocument.of(catalogJson), contextDocument).get();
        var expanded = JsonLd.expand(JsonDocument.of(compacted)).get();
        
        var fromJsonResult = transformerRegistry.transform(expanded.get(0), Catalog.class);
        
        assertThat(fromJsonResult.succeeded()).isTrue();
        var catalogTransformed = fromJsonResult.getContent();
        assertThat(catalogTransformed).isNotNull();
        
        assertCatalogsEqual(catalogTransformed);
    }
    
    private void assertCatalogsEqual(Catalog otherCatalog) {
        // compare catalog properties
        assertThat(catalog.getId()).isEqualTo(otherCatalog.getId());
        assertThat(catalog.getProperties().size())
                .isEqualTo(1)
                .isEqualTo(otherCatalog.getProperties().size());
        assertThat(catalog.getProperties().get(CATALOG_PROPERTY_KEY))
                .isEqualTo(otherCatalog.getProperties().get(CATALOG_PROPERTY_KEY));
    
        // compare data services
        assertThat(catalog.getDataServices().size())
                .isEqualTo(1)
                .isEqualTo(otherCatalog.getDataServices().size());
        var otherDataService = otherCatalog.getDataServices().get(0);
        assertThat(dataService.getId()).isEqualTo(otherDataService.getId());
        assertThat(dataService.getTerms()).isEqualTo(otherDataService.getTerms());
        assertThat(dataService.getEndpointUrl()).isEqualTo(otherDataService.getEndpointUrl());
    
        // compare dataset properties
        assertThat(catalog.getDatasets().size())
                .isEqualTo(1)
                .isEqualTo(otherCatalog.getDatasets().size());
        var otherDataset = otherCatalog.getDatasets().get(0);
        assertThat(dataset.getId()).isEqualTo(otherDataset.getId());
        assertThat(dataset.getProperties().size())
                .isEqualTo(1)
                .isEqualTo(otherDataset.getProperties().size());
        assertThat(dataset.getProperties().get(DATASET_PROPERTY_KEY))
                .isEqualTo(otherDataset.getProperties().get(DATASET_PROPERTY_KEY));
    
        assertThat(dataset.getOffers().size())
                .isEqualTo(1)
                .isEqualTo(otherDataset.getOffers().size());
        dataset.getOffers().keySet().forEach(k -> assertThat(otherDataset.getOffers()).containsKey(k));
        
        // compare dataset policies
        var otherPolicy = otherDataset.getOffers().values().stream().findFirst().get();
        assertThat(otherPolicy.getPermissions()).hasSize(1);
        assertThat(otherPolicy.getProhibitions()).isEmpty();
        assertThat(otherPolicy.getObligations()).isEmpty();
        
        var permission = policy.getPermissions().get(0);
        var otherPermission = otherPolicy.getPermissions().get(0);
        assertThat(permission.getAction().getType())
                .isEqualTo(otherPermission.getAction().getType());
        assertThat(otherPermission.getConstraints()).hasSize(1);
        
        var constraint = (AtomicConstraint) permission.getConstraints().get(0);
        var otherConstraint = (AtomicConstraint) otherPermission.getConstraints().get(0);
        
        assertThat(((LiteralExpression) constraint.getLeftExpression()).asString())
                .isEqualTo(((LiteralExpression) otherConstraint.getLeftExpression()).asString());
        assertThat(constraint.getOperator()).isEqualTo(otherConstraint.getOperator());
        assertThat(((LiteralExpression) constraint.getRightExpression()).asString())
                .isEqualTo(((LiteralExpression) otherConstraint.getRightExpression()).asString());
        
        // compare dataset distributions
        assertThat(dataset.getDistributions().size())
                .isEqualTo(1)
                .isEqualTo(otherDataset.getDistributions().size());
        var otherDistribution = otherDataset.getDistributions().get(0);
        assertThat(distribution.getFormat()).isEqualTo(otherDistribution.getFormat());
        assertThat(otherDistribution.getDataService()).isNull();
        assertThat(otherDistribution.getDataServiceId()).isNotNull()
                        .isEqualTo(distribution.getDataService().getId());
    }
    
    private Catalog getCatalog(Dataset dataset, DataService dataService) {
        return Catalog.Builder.newInstance()
                .id("catalog-id")
                .dataset(dataset)
                .dataService(dataService)
                .property(CATALOG_PROPERTY_KEY, "value")
                .build();
    }
    
    private DataService getDataService() {
        return DataService.Builder.newInstance()
                .id("data-service-id")
                .terms("terms")
                .endpointUrl("url")
                .build();
    }
    
    private Dataset getDataset(Distribution distribution, Policy policy) {
        return Dataset.Builder.newInstance()
                .id("dataset-id")
                .distribution(distribution)
                .offer("offer-id", policy)
                .property(DATASET_PROPERTY_KEY, "value")
                .build();
    }
    
    private Distribution getDistribution(DataService dataService) {
        return Distribution.Builder.newInstance()
                .format("format")
                .dataService(dataService)
                .build();
    }
    
    private Policy getPolicy() {
        return Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .constraint(AtomicConstraint.Builder.newInstance()
                                .leftExpression(new LiteralExpression("left"))
                                .operator(Operator.EQ)
                                .rightExpression(new LiteralExpression("right"))
                                .build())
                        .build())
                .build();
    }
}
