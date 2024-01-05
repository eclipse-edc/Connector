/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Mercedes-Benz Tech Innovation GmbH - DataEncrypter can be provided by extensions
 *
 */

package org.eclipse.edc.connector.transfer.dataplane;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.transfer.dataplane.api.ConsumerPullTransferTokenValidationApiController;
import org.eclipse.edc.connector.transfer.dataplane.flow.ConsumerPullTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.flow.ProviderPushTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullDataPlaneProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.connector.transfer.dataplane.spi.token.ConsumerPullTokenExpirationDateFunction;
import org.eclipse.edc.connector.transfer.dataplane.validation.ExpirationDateValidationRule;
import org.eclipse.edc.connector.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.jwt.JwtGenerationService;
import org.eclipse.edc.jwt.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.jwt.TokenValidationServiceImpl;
import org.eclipse.edc.jwt.spi.SignatureInfo;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.spi.WebService;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.function.Supplier;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;

@Extension(value = TransferDataPlaneCoreExtension.NAME)
public class TransferDataPlaneCoreExtension implements ServiceExtension {

    public static final String NAME = "Transfer Data Plane Core";

    @Inject
    private Vault vault;

    @Inject
    private WebService webService;

    @Inject
    private DataFlowManager dataFlowManager;

    @Inject
    private Clock clock;

    @Inject
    private DataEncrypter dataEncrypter;

    @Inject
    private ControlApiConfiguration controlApiConfiguration;

    @Inject
    private DataPlaneSelectorService selectorService;

    @Inject
    private DataPlaneClientFactory clientFactory;

    @Inject
    private ConsumerPullTokenExpirationDateFunction tokenExpirationDateFunction;

    @Inject(required = false)
    private ControlApiUrl callbackUrl;

    @Inject
    private TypeManager typeManager;

    @Inject
    private PublicKeyResolver publicKeyResolver;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var pubKeyAlias = context.getSetting(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, null);
        var privKeyAlias = context.getSetting(TOKEN_SIGNER_PRIVATE_KEY_ALIAS, null);

        var controller = new ConsumerPullTransferTokenValidationApiController(tokenValidationService(), dataEncrypter, typeManager);
        webService.registerResource(controlApiConfiguration.getContextAlias(), controller);

        var resolver = new ConsumerPullDataPlaneProxyResolver(dataEncrypter, typeManager, new JwtGenerationService(), getPrivateKeySupplier(context, privKeyAlias, pubKeyAlias), tokenExpirationDateFunction);
        dataFlowManager.register(new ConsumerPullTransferDataFlowController(selectorService, resolver));
        dataFlowManager.register(new ProviderPushTransferDataFlowController(callbackUrl, selectorService, clientFactory));

        dataAddressValidatorRegistry.registerDestinationValidator("HttpProxy", dataAddress -> ValidationResult.success());
    }

    @NotNull
    private Supplier<SignatureInfo> getPrivateKeySupplier(ServiceExtensionContext context, String privKeyAlias, String pubKeyAlias) {
        return () -> privateKeyResolver.resolvePrivateKey(privKeyAlias)
                .map(pk -> new SignatureInfo(pk, pubKeyAlias))
                .orElse(f -> {
                    context.getMonitor().warning(f.getFailureDetail());
                    return null;
                });
    }

    private TokenValidationService tokenValidationService() {
        var registry = new TokenValidationRulesRegistryImpl();
        registry.addRule(new ExpirationDateValidationRule(clock));
        return new TokenValidationServiceImpl(publicKeyResolver, registry);
    }

}
