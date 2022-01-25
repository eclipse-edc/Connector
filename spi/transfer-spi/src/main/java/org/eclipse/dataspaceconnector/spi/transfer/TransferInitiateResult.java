/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer;

import org.eclipse.dataspaceconnector.spi.response.ResponseFailure;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransferInitiateResult extends AbstractResult<String, ResponseFailure> {

    private @Nullable Object data;

    private TransferInitiateResult(String content, ResponseFailure failure) {
        super(content, failure);
    }

    public static TransferInitiateResult success(String id) {
        return new TransferInitiateResult(id, null);
    }

    public static TransferInitiateResult success(String id, Object data) {
        var successResult = success(id);
        successResult.data = data;

        return successResult;
    }

    public static TransferInitiateResult error(String id, ResponseStatus status, String... errors) {
        var e = Stream.of(errors).filter(Objects::nonNull).collect(Collectors.toList());
        return new TransferInitiateResult(id, new ResponseFailure(status, e));
    }

    public @Nullable Object getData() {
        return data;
    }
}
