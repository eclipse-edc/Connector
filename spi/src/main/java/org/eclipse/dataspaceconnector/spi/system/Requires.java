package org.eclipse.dataspaceconnector.spi.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies which feature a certain class, package or modules requires.
 * Feature must be namespaced in the form "edc:XXX:YYY:ZZZ"
 */
@Target({ ElementType.TYPE, ElementType.PACKAGE, ElementType.MODULE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Requires {
    Class<?>[] value();
}
