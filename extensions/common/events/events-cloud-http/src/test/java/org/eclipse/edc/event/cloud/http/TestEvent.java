/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.event.cloud.http;

import org.eclipse.edc.spi.event.Event;

class TestEvent extends Event {
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String name() {
        return "test";
    }


    public static class Builder {

        private final TestEvent event;

        private Builder() {
            event = new TestEvent();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder data(String data) {
            event.data = data;
            return this;
        }

        public TestEvent build() {
            return event;
        }
    }

}
