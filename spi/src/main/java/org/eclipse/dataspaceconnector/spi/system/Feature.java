package org.eclipse.dataspaceconnector.spi.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes the name of the feature, that extensions can @Provide or @Require.
 * Feature must be namespaced in the form "edc:XXX:YYY:ZZZ"
 */
@Target({ ElementType.TYPE, ElementType.PACKAGE, ElementType.MODULE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Feature {
    String value() default "";
}
