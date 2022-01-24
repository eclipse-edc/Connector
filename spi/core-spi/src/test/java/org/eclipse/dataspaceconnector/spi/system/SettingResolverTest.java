package org.eclipse.dataspaceconnector.spi.system;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingResolverTest {

    @Test
    void get_setting_returns_an_int_value_if_default_is_an_integer() {
        var resolver = getSettingResolver((setting, defaultValue) -> "5");

        var setting = resolver.getSetting("key", 1);

        assertThat(setting).isEqualTo(5);
    }

    @Test
    void get_setting_returns_default_as_an_int_value_if_setting_is_not_found() {
        var resolver = getSettingResolver((setting, defaultValue) -> defaultValue);

        var setting = resolver.getSetting("key", 1);

        assertThat(setting).isEqualTo(1);
    }

    @Test
    void get_setting_throws_exception_if_the_value_is_not_a_valid_integer() {
        var resolver = getSettingResolver((setting, defaultValue) -> "not an integer");

        assertThatThrownBy(() -> resolver.getSetting("key", 2)).isInstanceOf(EdcException.class);
    }

    @Test
    void get_setting_returns_a_long_value_if_default_is_a_long() {
        var resolver = getSettingResolver((setting, defaultValue) -> "5");

        var setting = resolver.getSetting("key", 1L);

        assertThat(setting).isEqualTo(5L);
    }

    @Test
    void get_setting_returns_default_as_a_long_value_if_setting_is_not_found() {
        var resolver = getSettingResolver((setting, defaultValue) -> defaultValue);

        var setting = resolver.getSetting("key", 1L);

        assertThat(setting).isEqualTo(1L);
    }

    @Test
    void get_setting_throws_exception_if_the_value_is_not_a_valid_long() {
        var resolver = getSettingResolver((setting, defaultValue) -> "not a long");

        assertThatThrownBy(() -> resolver.getSetting("key", 2L)).isInstanceOf(EdcException.class);
    }

    @NotNull
    private SettingResolver getSettingResolver(BiFunction<String, String, String> getSetting) {
        return new SettingResolver() {

            @Override
            public String getSetting(String setting, String defaultValue) {
                return getSetting.apply(setting, defaultValue);
            }

            @Override
            public Map<String, Object> getSettings(String prefix) {
                return null;
            }
        };
    }
}