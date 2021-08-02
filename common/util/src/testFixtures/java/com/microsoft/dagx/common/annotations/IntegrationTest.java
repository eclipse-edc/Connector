package com.microsoft.dagx.common.annotations;

import org.junit.jupiter.api.Test;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Test
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TEST", matches = "true")
public @interface IntegrationTest {
}
