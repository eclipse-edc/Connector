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

package org.eclipse.edc.protocol.dsp.transform;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.transformer.OdrlTransformersFactory;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromCatalogTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToCatalogTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDataServiceTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDatasetTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToDistributionTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

/**
 * Provides support for transforming DCAT catalog and ODRL policy types to and from JSON-LD. The
 * respective transformers are registered with the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspTransformExtension.NAME)
public class DspTransformExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Transform Extension";

    @Inject
    private TypeManager typeManager;

    @Inject
    private TypeTransformerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = typeManager.getMapper(JSON_LD);
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class);

        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());

        // EDC model to JSON-LD transformers
        registry.register(new JsonObjectFromCatalogTransformer(jsonBuilderFactory, mapper));
        registry.register(new JsonObjectFromDatasetTransformer(jsonBuilderFactory, mapper));
        registry.register(new JsonObjectFromPolicyTransformer(jsonBuilderFactory));
        registry.register(new JsonObjectFromDistributionTransformer(jsonBuilderFactory));
        registry.register(new JsonObjectFromDataServiceTransformer(jsonBuilderFactory));
        registry.register(new JsonObjectFromAssetTransformer(jsonBuilderFactory, mapper));
        registry.register(new JsonObjectFromQuerySpecTransformer(jsonBuilderFactory));
        registry.register(new JsonObjectFromCriterionTransformer(jsonBuilderFactory, mapper));

        // JSON-LD to EDC model transformers
        // DCAT transformers
        registry.register(new JsonObjectToCatalogTransformer());
        registry.register(new JsonObjectToDataServiceTransformer());
        registry.register(new JsonObjectToDatasetTransformer());
        registry.register(new JsonObjectToDistributionTransformer());

        // ODRL Transformers
        OdrlTransformersFactory.jsonObjectToOdrlTransformers().forEach(registry::register);

        registry.register(new JsonValueToGenericTypeTransformer(mapper));
        registry.register(new JsonObjectToAssetTransformer());
        registry.register(new JsonObjectToQuerySpecTransformer());
        registry.register(new JsonObjectToCriterionTransformer());

    }
}
