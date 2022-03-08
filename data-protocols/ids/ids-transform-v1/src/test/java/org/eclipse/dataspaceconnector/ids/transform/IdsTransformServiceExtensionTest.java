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

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.LeftOperand;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.Connector;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(DependencyInjectionExtension.class)
class IdsTransformServiceExtensionTest {

    private Map<Class<?>, List<Class<?>>> knownConvertibles;
    private IdsTransformServiceExtension idsTransformServiceExtension;

    private ServiceExtensionContext serviceExtensionContext;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        knownConvertibles = new HashMap<>();

        var transformerRegistry = new TestTransformerRegistry(knownConvertibles);
        context.registerService(TransformerRegistry.class, transformerRegistry);
        idsTransformServiceExtension = factory.constructInstance(IdsTransformServiceExtension.class);
        serviceExtensionContext = context;
    }

    @ParameterizedTest(name = "[{index}] can transform {0} to {1}")
    @ArgumentsSource(VerifyRequiredTransformerRegisteredArgumentsProvider.class)
    void verifyRequiredTransformerRegistered(Class<?> inputType, Class<?> outputType) {
        idsTransformServiceExtension.initialize(serviceExtensionContext);

        assertThat(knownConvertibles).containsKey(inputType);
        assertThat(knownConvertibles).extracting((m) -> m.get(inputType)).isNotNull();
        assertThat(knownConvertibles.get(inputType)).contains(outputType);
    }

    static class VerifyRequiredTransformerRegisteredArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.arguments(Action.class, de.fraunhofer.iais.eis.Action.class),
                    Arguments.arguments(Artifact.class, Asset.class),
                    Arguments.arguments(Asset.class, Artifact.class),
                    Arguments.arguments(Asset.class, Representation.class),
                    Arguments.arguments(Asset.class, Resource.class),
                    Arguments.arguments(BinaryOperator.class, Operator.class),
                    Arguments.arguments(Connector.class, de.fraunhofer.iais.eis.Connector.class),
                    Arguments.arguments(Constraint.class, de.fraunhofer.iais.eis.Constraint.class),
                    Arguments.arguments(ContractOffer.class, de.fraunhofer.iais.eis.ContractOffer.class),
                    Arguments.arguments(ContractAgreement.class, de.fraunhofer.iais.eis.ContractAgreement.class),
                    Arguments.arguments(Catalog.class, ResourceCatalog.class),
                    Arguments.arguments(de.fraunhofer.iais.eis.Constraint.class, Constraint.class),
                    Arguments.arguments(de.fraunhofer.iais.eis.ContractOffer.class, ContractOffer.class),
                    Arguments.arguments(de.fraunhofer.iais.eis.Permission.class, Permission.class),
                    Arguments.arguments(de.fraunhofer.iais.eis.Prohibition.class, Prohibition.class),
                    Arguments.arguments(Duty.class, de.fraunhofer.iais.eis.Duty.class),
                    Arguments.arguments(Expression.class, LeftOperand.class),
                    Arguments.arguments(Expression.class, RdfResource.class),
                    Arguments.arguments(IdsId.class, URI.class),
                    Arguments.arguments(LeftOperand.class, Expression.class),
                    Arguments.arguments(OfferedAsset.class, Resource.class),
                    Arguments.arguments(Operator.class, BinaryOperator.class),
                    Arguments.arguments(Permission.class, de.fraunhofer.iais.eis.Permission.class),
                    Arguments.arguments(Prohibition.class, de.fraunhofer.iais.eis.Prohibition.class),
                    Arguments.arguments(RdfResource.class, Expression.class),
                    Arguments.arguments(Representation.class, Asset.class),
                    Arguments.arguments(Resource.class, Asset.class),
                    Arguments.arguments(ResourceCatalog.class, Catalog.class),
                    Arguments.arguments(SecurityProfile.class, de.fraunhofer.iais.eis.SecurityProfile.class),
                    Arguments.arguments(URI.class, IdsId.class)
            );
        }
    }

    private static class TestTransformerRegistry implements TransformerRegistry {
        private final Map<Class<?>, List<Class<?>>> knownConvertibles;

        public TestTransformerRegistry(Map<Class<?>, List<Class<?>>> knownConvertibles) {
            this.knownConvertibles = knownConvertibles;
        }

        @Override
        public void register(IdsTypeTransformer<?, ?> transformer) {
            Objects.requireNonNull(transformer.getInputType());
            Objects.requireNonNull(transformer.getOutputType());

            knownConvertibles.computeIfAbsent(
                            transformer.getInputType(),
                            (k) -> new LinkedList<>())
                    .add(transformer.getOutputType());
        }

        @Override
        public <INPUT, OUTPUT> Result<OUTPUT> transform(@NotNull INPUT object, @NotNull Class<OUTPUT> outputType) {
            throw new RuntimeException("Not intended to be used within this Test");
        }
    }
}
