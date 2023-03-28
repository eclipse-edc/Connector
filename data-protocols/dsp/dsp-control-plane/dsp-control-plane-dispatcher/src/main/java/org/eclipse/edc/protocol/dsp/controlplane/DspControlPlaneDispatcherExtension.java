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

package org.eclipse.edc.protocol.dsp.controlplane;

import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.controlplane.delegate.ContractAgreementDelegate;
import org.eclipse.edc.protocol.dsp.controlplane.delegate.ContractRequestDelegate;
import org.eclipse.edc.protocol.dsp.controlplane.delegate.ContractTerminationDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspRemoteMessageDispatcher;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = DspControlPlaneDispatcherExtension.NAME)
public class DspControlPlaneDispatcherExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol: Control Plane Dispatcher Extension";

    @Inject
    private DspRemoteMessageDispatcher messageDispatcher;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLdTransformerRegistry transformerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var objectMapper = typeManager.getMapper("json-ld");

        messageDispatcher.registerDelegate(new ContractAgreementDelegate(objectMapper, transformerRegistry));
        messageDispatcher.registerDelegate(new ContractRequestDelegate(objectMapper, transformerRegistry));
        messageDispatcher.registerDelegate(new ContractTerminationDelegate(objectMapper, transformerRegistry));
        // TODO implement ContractAgreementVerificationDelegate and ContractNegotiationEventMessageDelegate (https://github.com/eclipse-edc/Connector/pull/2601)
    }
}
