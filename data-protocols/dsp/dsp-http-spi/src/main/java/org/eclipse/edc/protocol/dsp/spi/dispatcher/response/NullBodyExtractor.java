/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.dispatcher.response;

import okhttp3.ResponseBody;

/**
 * Do not extract the body from response and return null.
 */
public class NullBodyExtractor implements DspHttpResponseBodyExtractor<Object> {
    @Override
    public Object extractBody(ResponseBody response) {
        return null;
    }
}
