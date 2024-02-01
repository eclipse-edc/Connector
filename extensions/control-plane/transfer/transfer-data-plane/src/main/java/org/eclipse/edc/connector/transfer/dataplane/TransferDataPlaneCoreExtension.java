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
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.iam.LocalPublicKeyService;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.spi.WebService;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.function.Supplier;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;

@Extension(value = TransferDataPlaneCoreExtension.NAME)
public class TransferDataPlaneCoreExtension implements ServiceExtension {

    public static final String NAME = "Transfer Data Plane Core";
    public static final String TRANSFER_DATAPLANE_TOKEN_CONTEXT = "dataplane-transfer";

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
    private LocalPublicKeyService publicKeyService;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;

    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;

    @Inject
    private TokenValidationService tokenValidationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var publicKeyAlias = context.getSetting(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, null);
        var privateKeyAlias = context.getSetting(TOKEN_SIGNER_PRIVATE_KEY_ALIAS, null);

        if (publicKeyAlias != null && privateKeyAlias != null) {
            context.getMonitor().info("One of these settings is not configured, so the connector won't be able to provide 'consumer-pull' transfers: [%s, %s]"
                            .formatted(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, TOKEN_SIGNER_PRIVATE_KEY_ALIAS));

            var controller = new ConsumerPullTransferTokenValidationApiController(tokenValidationService, dataEncrypter, typeManager, (i) -> publicKeyService.resolveKey(publicKeyAlias));
            webService.registerResource(controlApiConfiguration.getContextAlias(), controller);

            var resolver = new ConsumerPullDataPlaneProxyResolver(dataEncrypter, typeManager, new JwtGenerationService(), getPrivateKeySupplier(context, privateKeyAlias), () -> publicKeyAlias, tokenExpirationDateFunction);
            dataFlowManager.register(new ConsumerPullTransferDataFlowController(selectorService, resolver));
        }

        tokenValidationRulesRegistry.addRule(TRANSFER_DATAPLANE_TOKEN_CONTEXT, new ExpirationDateValidationRule(clock));

        dataFlowManager.register(new ProviderPushTransferDataFlowController(callbackUrl, selectorService, clientFactory));
        dataAddressValidatorRegistry.registerDestinationValidator("HttpProxy", dataAddress -> ValidationResult.success());
    }

    @NotNull
    private Supplier<PrivateKey> getPrivateKeySupplier(ServiceExtensionContext context, String privateKeyAlias) {
        return () -> privateKeyResolver.resolvePrivateKey(privateKeyAlias)
                .orElse(f -> {
                    context.getMonitor().warning("Cannot resolve private key: " + f.getFailureDetail());
                    return null;
                });
    }

}
