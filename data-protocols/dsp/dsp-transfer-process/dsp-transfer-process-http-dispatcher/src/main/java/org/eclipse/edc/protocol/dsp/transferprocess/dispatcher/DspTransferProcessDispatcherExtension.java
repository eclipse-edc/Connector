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

package org.eclipse.edc.protocol.dsp.transferprocess.dispatcher;


import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.delegate.TransferCompletionDelegate;
import org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.delegate.TransferRequestDelegate;
import org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.delegate.TransferStartDelegate;
import org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.delegate.TransferTerminationDelegate;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static org.eclipse.edc.jsonld.JsonLdExtension.TYPE_MANAGER_CONTEXT_JSON_LD;


/**
 *
 * Provides HTTP dispatching for Dataspace Protocol transfer process messages via the {@link DspHttpRemoteMessageDispatcher}.
 */
@Extension(value = DspTransferProcessDispatcherExtension.NAME)
public class DspTransferProcessDispatcherExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Transfer HTTP Dispatcher Extension";

    @Inject
    private DspHttpRemoteMessageDispatcher messageDispatcher;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLdRemoteMessageSerializer remoteMessageSerializer;
    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        messageDispatcher.registerDelegate(new TransferRequestDelegate(remoteMessageSerializer, typeManager.getMapper(TYPE_MANAGER_CONTEXT_JSON_LD), transformerRegistry));
        messageDispatcher.registerDelegate(new TransferCompletionDelegate(remoteMessageSerializer));
        messageDispatcher.registerDelegate(new TransferStartDelegate(remoteMessageSerializer));
        messageDispatcher.registerDelegate(new TransferTerminationDelegate(remoteMessageSerializer));
    }
}
