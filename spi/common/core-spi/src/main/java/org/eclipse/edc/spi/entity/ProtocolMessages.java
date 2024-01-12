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

package org.eclipse.edc.spi.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe the history of the wire messages sent or received.
 */
public class ProtocolMessages {

    private String lastSent;
    private final List<String> received = new ArrayList<>();

    public void setLastSent(String lastSent) {
        this.lastSent = lastSent;
    }

    public String getLastSent() {
        return lastSent;
    }

    public void addReceived(String received) {
        this.received.add(received);
    }

    public boolean isAlreadyReceived(String id) {
        return received.contains(id);
    }

    public List<String> getReceived() {
        return received;
    }

    public void setReceived(List<String> received) {
        this.received.clear();
        this.received.addAll(received);
    }
}
