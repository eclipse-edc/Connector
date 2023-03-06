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

package org.eclipse.edc.protocol.dsp.spi.dispatcher;

import java.util.function.Function;

import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

public interface DspDispatcherDelegate<M extends RemoteMessage, R> {

    Class<M> getMessageType();
    
    Request buildRequest(M message);
    
    Function<Response, R> parseResponse();

}
