package org.eclipse.dataspaceconnector.common.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationFunctionsTest {

    private Map<String, Object> allProperties;

    @BeforeEach
    void setUp() {
        allProperties = Map.of(
                "edc.datasource.default.user", "test-user",
                "edc.datasource.default.password", "test-pwd",
                "edc.datasource.default.driverClassName", "org.company.default.Driver",
                "edc.datasource.another.user", "test-user2",
                "edc.datasource.another.password", "test-pwd2",
                "edc.datasource.another.driverClassName", "org.company.another.Driver",
                "edc.datasource.another.sub.property2", "bar");
    }

    @Test
    void hierarchical() {
        var hierarchy = ConfigurationFunctions.hierarchical(allProperties);
        assertThat(hierarchy).hasSize(1);
        assertThat(hierarchy).containsKey("edc");
    }

    @Test
    void hierarchical_withRoot() {
        var hierarchy = ConfigurationFunctions.hierarchical(allProperties, "edc.datasource");

        assertThat(hierarchy).hasSize(2);
        assertThat(hierarchy.get("default")).containsExactlyInAnyOrderEntriesOf(Map.of(
                "user", "test-user",
                "password", "test-pwd",
                "driverClassName", "org.company.default.Driver"));

        assertThat(hierarchy.get("another")).containsExactlyInAnyOrderEntriesOf(Map.of(
                "user", "test-user2",
                "password", "test-pwd2",
                "sub.property2", "bar",
                "driverClassName", "org.company.another.Driver"));
    }

    @Test
    void hierarchical_withRoot_deeperLevel() {

        var hierarchy = ConfigurationFunctions.hierarchical(allProperties, "edc.datasource.default");

        assertThat(hierarchy).hasSize(1);
        assertThat(hierarchy.get("edc.datasource.default")).containsExactlyInAnyOrderEntriesOf(Map.of(
                "user", "test-user",
                "password", "test-pwd",
                "driverClassName", "org.company.default.Driver"));
    }
}