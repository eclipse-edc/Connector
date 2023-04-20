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

package org.eclipse.edc.protocol.dsp.negotiation.dispatcher;

import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate.ContractAgreementMessageDelegate;
import org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate.ContractAgreementVerificationMessageDelegate;
import org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate.ContractNegotiationEventMessageDelegate;
import org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate.ContractOfferRequestDelegate;
import org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate.ContractRejectionHttpDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.jsonld.JsonLdExtension.TYPE_MANAGER_CONTEXT_JSON_LD;

@Extension(value = DspNegotiationHttpDispatcherExtension.NAME)
public class DspNegotiationHttpDispatcherExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Negotiation HTTP Dispatcher Extension";

    @Inject
    private DspHttpRemoteMessageDispatcher messageDispatcher;
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
        var objectMapper = typeManager.getMapper(TYPE_MANAGER_CONTEXT_JSON_LD);

        messageDispatcher.registerDelegate(new ContractAgreementMessageDelegate(objectMapper, transformerRegistry));
        messageDispatcher.registerDelegate(new ContractAgreementVerificationMessageDelegate(objectMapper, transformerRegistry));
        messageDispatcher.registerDelegate(new ContractNegotiationEventMessageDelegate(objectMapper, transformerRegistry));
        messageDispatcher.registerDelegate(new ContractOfferRequestDelegate(objectMapper, transformerRegistry));
        messageDispatcher.registerDelegate(new ContractRejectionHttpDelegate(objectMapper, transformerRegistry));
    }
}
