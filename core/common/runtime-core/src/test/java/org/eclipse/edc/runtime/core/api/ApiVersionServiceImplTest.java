/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.runtime.core.api;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

class ApiVersionServiceImplTest {

    private final ApiVersionService service = new ApiVersionServiceImpl(new JacksonTypeManager());

    @Nested
    class RegisterVersionInfo {

        @Test
        void shouldDeserializeJsonAndAddRecords() {
            var json = """
                    [
                        {
                            "version": "1.0.0",
                            "urlPath": "/path",
                            "lastUpdated": "2026-03-23T08:43:01Z",
                            "maturity": "stable"
                          }
                    ]
                    """;

            service.registerVersionInfo("theApiContext", new ByteArrayInputStream(json.getBytes()));

            assertThat(service.getRecords()).hasSize(1).extracting("theApiContext")
                    .asInstanceOf(list(VersionRecord.class)).hasSize(1).first().satisfies(record -> {
                        assertThat(record.version()).isEqualTo("1.0.0");
                        assertThat(record.urlPath()).isEqualTo("/path");
                        assertThat(record.lastUpdated()).isEqualTo("2026-03-23T08:43:01Z");
                        assertThat(record.maturity()).isEqualTo("stable");
                    });
        }

        @Test
        void shouldDeserializeSingleItemWithoutArray() {
            var json = """
                    {
                        "version": "1.0.0",
                        "urlPath": "/path",
                        "lastUpdated": "2026-03-23T08:43:01Z",
                        "maturity": "stable"
                      }
                    """;

            service.registerVersionInfo("theApiContext", new ByteArrayInputStream(json.getBytes()));

            assertThat(service.getRecords()).hasSize(1);
        }
    }

    @Nested
    class AddRecord {

        @Test
        void addRecord_whenNotExists() {
            service.addRecord("foo", new VersionRecord("1.0.0", "/v1/", Instant.now(), "stable"));
            assertThat(service.getRecords()).hasSize(1).containsKey("foo");
            assertThat(service.getRecords().get("foo")).hasSize(1);
        }

        @Test
        void addRecord_whenExists() {
            service.addRecord("foo", new VersionRecord("1.0.0", "/v1/", Instant.now(), "stable"));
            service.addRecord("foo", new VersionRecord("2.0.0", "/v2/", Instant.now(), "stable"));
            assertThat(service.getRecords()).hasSize(1).containsKey("foo");
            assertThat(service.getRecords().get("foo")).hasSize(2);
        }

        @Test
        void getRecords() {
            service.addRecord("foo1", new VersionRecord("1.0.0", "/v1/", Instant.now(), "stable"));
            service.addRecord("foo2", new VersionRecord("2.0.0", "/v2/", Instant.now(), "stable"));
            assertThat(service.getRecords()).hasSize(2);
        }

    }
}
