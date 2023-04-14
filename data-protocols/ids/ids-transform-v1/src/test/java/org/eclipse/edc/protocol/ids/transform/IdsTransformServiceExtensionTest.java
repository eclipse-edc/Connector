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

package org.eclipse.edc.protocol.ids.transform;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.ids.spi.domain.connector.Connector;
import org.eclipse.edc.protocol.ids.spi.domain.connector.SecurityProfile;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.protocol.ids.spi.types.container.OfferedAsset;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@ExtendWith(DependencyInjectionExtension.class)
class IdsTransformServiceExtensionTest {

    private IdsTransformServiceExtension idsTransformServiceExtension;

    private ServiceExtensionContext serviceExtensionContext;
    private final IdsTransformerRegistry transformerRegistry = mock(IdsTransformerRegistry.class);

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(IdsTransformerRegistry.class, transformerRegistry);
        idsTransformServiceExtension = factory.constructInstance(IdsTransformServiceExtension.class);
        serviceExtensionContext = context;
    }

    @ParameterizedTest(name = "[{index}] can transform {0} to {1}")
    @ArgumentsSource(VerifyRequiredTransformerRegisteredArgumentsProvider.class)
    void verifyRequiredTransformerRegistered(Class<?> inputType, Class<?> outputType) {
        idsTransformServiceExtension.initialize(serviceExtensionContext);

        verify(transformerRegistry).register(argThat(t -> t.getInputType().equals(inputType) && t.getOutputType().equals(outputType)));
    }

    private static class VerifyRequiredTransformerRegisteredArgumentsProvider implements ArgumentsProvider {
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
                    Arguments.arguments(ContractAgreementMessage.class, de.fraunhofer.iais.eis.ContractAgreement.class),
                    Arguments.arguments(Catalog.class, ResourceCatalog.class),
                    Arguments.arguments(de.fraunhofer.iais.eis.Constraint.class, Constraint.class),
                    Arguments.arguments(de.fraunhofer.iais.eis.Permission.class, Permission.class),
                    Arguments.arguments(de.fraunhofer.iais.eis.Prohibition.class, Prohibition.class),
                    Arguments.arguments(Duty.class, de.fraunhofer.iais.eis.Duty.class),
                    Arguments.arguments(Expression.class, String.class),
                    Arguments.arguments(Expression.class, RdfResource.class),
                    Arguments.arguments(String.class, Expression.class),
                    Arguments.arguments(OfferedAsset.class, Resource.class),
                    Arguments.arguments(Operator.class, BinaryOperator.class),
                    Arguments.arguments(Permission.class, de.fraunhofer.iais.eis.Permission.class),
                    Arguments.arguments(Prohibition.class, de.fraunhofer.iais.eis.Prohibition.class),
                    Arguments.arguments(RdfResource.class, Expression.class),
                    Arguments.arguments(Representation.class, Asset.class),
                    Arguments.arguments(Resource.class, Asset.class),
                    Arguments.arguments(ResourceCatalog.class, Catalog.class),
                    Arguments.arguments(SecurityProfile.class, de.fraunhofer.iais.eis.SecurityProfile.class)
            );
        }
    }
}
