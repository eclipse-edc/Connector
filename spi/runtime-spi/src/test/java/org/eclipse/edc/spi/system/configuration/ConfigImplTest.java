/*
 *  Copyright (c) 2022 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Initial Implementation
 *
 */

package org.eclipse.edc.spi.system.configuration;

import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigImplTest {

    @Test
    void getStringConfig() {
        var config = new ConfigImpl("", Map.of("key", "value"));

        var value = config.getString("key");

        assertThat(value).isEqualTo("value");
    }

    @Test
    void getStringShouldThrowExceptionIfKeyNotFoundAndNoDefaultDefined() {
        var config = new ConfigImpl("", emptyMap());

        assertThatThrownBy(() -> config.getString("key")).isInstanceOf(EdcException.class);
    }

    @Test
    void getStringShouldReturnDefaultIfKeyNotFound() {
        var config = new ConfigImpl("", emptyMap());

        var value = config.getString("key", "default");

        assertThat(value).isEqualTo("default");
    }

    @Test
    void getStringShouldReturnNullIfDefaultIsNull() {
        var config = new ConfigImpl("", emptyMap());

        var value = config.getString("key", null);

        assertThat(value).isEqualTo(null);
    }

    @Test
    void getIntegerConfig() {
        var config = new ConfigImpl("", Map.of("key", "1"));

        var value = config.getInteger("key");

        assertThat(value).isEqualTo(1);
    }

    @Test
    void getIntegerShouldThrowExceptionIfKeyNotFoundAndNoDefaultDefined() {
        var config = new ConfigImpl("", emptyMap());

        assertThatThrownBy(() -> config.getInteger("key")).isInstanceOf(EdcException.class);
    }

    @Test
    void getIntegerShouldThrowExceptionIfValueIsNotValidInteger() {
        var config = new ConfigImpl("", Map.of("key", "not_an_integer"));

        assertThatThrownBy(() -> config.getInteger("key")).isInstanceOf(EdcException.class);
    }

    @Test
    void getIntegerShouldReturnDefaultIfKeyNotFound() {
        var config = new ConfigImpl("", emptyMap());

        var value = config.getInteger("key", 2);

        assertThat(value).isEqualTo(2);
    }

    @Test
    void getIntegerShouldReturnNullIfDefaultIsNull() {
        var config = new ConfigImpl("", emptyMap());

        var value = config.getInteger("key", null);

        assertThat(value).isEqualTo(null);
    }

    @Test
    void getLongConfig() {
        var config = new ConfigImpl("", Map.of("key", "1"));

        var value = config.getLong("key");

        assertThat(value).isEqualTo(1);
    }

    @Test
    void getLongShouldThrowExceptionIfKeyNotFoundAndNoDefaultDefined() {
        var config = new ConfigImpl("", emptyMap());

        assertThatThrownBy(() -> config.getLong("key")).isInstanceOf(EdcException.class);
    }

    @Test
    void getLongShouldThrowExceptionIfValueIsNotValidLong() {
        var config = new ConfigImpl("", Map.of("key", "not_a_long"));

        assertThatThrownBy(() -> config.getLong("key")).isInstanceOf(EdcException.class);
    }

    @Test
    void getLongShouldReturnDefaultIfKeyNotFound() {
        var config = new ConfigImpl("", emptyMap());

        var value = config.getLong("key", 2L);

        assertThat(value).isEqualTo(2);
    }

    @Test
    void getLongShouldReturnNullIfDefaultIsNull() {
        var config = new ConfigImpl("", emptyMap());

        var value = config.getLong("key", null);

        assertThat(value).isEqualTo(null);
    }

    @Test
    void getBooleanConfig() {
        var config = new ConfigImpl("", Map.of("key", "true"));

        var value = config.getBoolean("key");

        assertThat(value).isEqualTo(true);
    }

    @Test
    void getBooleanShouldThrowExceptionIfKeyNotFoundAndNoDefaultDefined() {
        var config = new ConfigImpl("", emptyMap());

        assertThatThrownBy(() -> config.getBoolean("key")).isInstanceOf(EdcException.class);
    }

    @Test
    void getBooleanShouldThrowExceptionIfValueIsNotValidBoolean() {
        var config = new ConfigImpl("", Map.of("key", "not_a_boolean"));

        assertThatThrownBy(() -> config.getBoolean("key")).isInstanceOf(EdcException.class);
    }

    @Test
    void getBooleanShouldReturnDefaultIfKeyNotFound() {
        var config = new ConfigImpl("", emptyMap());

        var value = config.getBoolean("key", true);

        assertThat(value).isEqualTo(true);
    }

    @Test
    void getBooleanShouldReturnNullIfDefaultIsNull() {
        var config = new ConfigImpl("", emptyMap());

        var value = config.getBoolean("key", null);

        assertThat(value).isEqualTo(null);
    }

    @Test
    void configPlusAnotherShouldReturnTheUnionOfTheEntriesOverwritingTheDuplicates() {
        var config1 = new ConfigImpl("", Map.of("key1", "value1", "key2", "value1"));
        var config2 = new ConfigImpl("", Map.of("key2", "value2", "key3", "value2"));

        var union = config1.merge(config2);

        assertThat(union.getString("key1")).isEqualTo("value1");
        assertThat(union.getString("key2")).isEqualTo("value2");
        assertThat(union.getString("key3")).isEqualTo("value2");
    }

    @Test
    void configPlusAnotherShouldReturnTheUnionOfTheEntriesOnTheRootPath() {
        var config1 = new ConfigImpl("group", Map.of("group.key1", "value1"));
        var config2 = new ConfigImpl("another.group", Map.of("another.group.key2", "value2"));

        var union = config1.merge(config2);

        assertThat(config1.getString("key1")).isEqualTo("value1");
        assertThatThrownBy(() -> union.getString("key1")).isInstanceOf(EdcException.class);
        assertThat(union.getString("group.key1")).isEqualTo("value1");
        assertThat(config2.getString("key2")).isEqualTo("value2");
        assertThatThrownBy(() -> union.getString("key2")).isInstanceOf(EdcException.class);
        assertThat(union.getString("another.group.key2")).isEqualTo("value2");
    }

    @Test
    void getConfigShouldReturnConfigRelativeToThePathSpecified() {
        var config = new ConfigImpl("", Map.of("group.subgroup.key", "value"));

        var value = config.getConfig("group").getConfig("subgroup").getString("key");

        assertThat(value).isEqualTo("value");
    }

    @Test
    void getConfigShouldReturnConfigRelativeToTheMultiLevelPathSpecified() {
        var config = new ConfigImpl("", Map.of("group.subgroup.key", "value"));

        var value = config.getConfig("group.subgroup").getString("key");

        assertThat(value).isEqualTo("value");
    }

    @Test
    void getConfigShouldFilterOutEntriesThatDoesNotBelongToTheGroup() {
        var map = Map.of("another.group.another.key", "anotherValue", "group.subgroup.key", "value");
        var config = new ConfigImpl("", map);

        var entries = config.getConfig("group.subgroup").getEntries();

        assertThat(entries).containsExactly(Map.entry("group.subgroup.key", "value"));
    }

    @Test
    void getConfigShouldGetOnlyEqualGroups() {
        var map = Map.of("group.key", "value", "group", "sameValue", "group2.key", "anotherValue");
        var config = new ConfigImpl("", map);

        var entries = config.getConfig("group").getEntries();

        assertThat(entries).containsExactly(Map.entry("group.key", "value"), Map.entry("group", "sameValue"));
    }

    @Test
    void getConfigWithEmptyStringShouldReturnAllTheConfig() {
        var map = Map.of("group.key", "value", "group", "sameValue", "group2.key", "anotherValue");
        var config = new ConfigImpl("", map);

        var entries = config.getConfig("").getEntries();

        assertThat(entries).containsExactlyInAnyOrderEntriesOf(map);
    }

    @Test
    void getEntriesShouldReturnAllTheEntriesWithTheWholePath() {
        var config = new ConfigImpl("", Map.of("group.subgroup.key", "value"));

        var entries = config.getEntries();

        assertThat(entries).containsExactly(entry("group.subgroup.key", "value"));
    }

    @Test
    void getRelativeEntriesShouldGiveTheEntriesRelativeToThePathSpecified() {
        var config = new ConfigImpl("", Map.of("group.subgroup.key", "value"));

        var entries = config.getConfig("group").getRelativeEntries();

        assertThat(entries).containsExactly(entry("subgroup.key", "value"));
    }

    @Test
    void getRelativeEntriesAtRootShouldEqualToEntries() {
        var config = new ConfigImpl("", Map.of("group.subgroup.key", "value"));

        var entries = config.getRelativeEntries();

        assertThat(entries).isEqualTo(config.getEntries());
    }

    @Test
    void getRelativeEntriesWithSpecifiedBasePathFiltersOut() {
        var config = new ConfigImpl("default", Map.of("default.properties.key", "value", "default.other", "anotherValue"));

        var entries = config.getRelativeEntries("properties");

        assertThat(entries).containsExactly(entry("properties.key", "value"));
    }

    @Test
    void groupByPathShouldGiveListOfSubConfigs() {
        var entries = Map.of("group.default.key", "defaultValue", "group.specific.key", "specificValue");
        var config = new ConfigImpl("group", entries);

        var configs = config.partition();

        List<Config> configList = configs.collect(toList());
        assertThat(configList).hasSize(2);
        assertThat(configList.get(0).getString("key")).isEqualTo("defaultValue");
        assertThat(configList.get(1).getString("key")).isEqualTo("specificValue");
    }

    @Test
    void getCurrentNodeReturnsTheNameOfTheLastLevel() {
        var config = new ConfigImpl("group.subgroup", emptyMap());

        var node = config.currentNode();

        assertThat(node).isEqualTo("subgroup");
    }

    @Test
    void hasPath_returnsTrueIfPathExists() {
        var config = new ConfigImpl(Map.of("example.path.key", "val", "example.path.otherkey", "val"));

        boolean hasPath = config.hasPath("example.path");

        assertThat(hasPath).isTrue();
    }

    @Test
    void hasPath_returnsFalseIfPathDoesNotExist() {
        var config = new ConfigImpl(Map.of("example.path.key", "val", "example.path.otherkey", "val"));

        boolean hasPath = config.hasPath("example.another.path");

        assertThat(hasPath).isFalse();
    }
}
