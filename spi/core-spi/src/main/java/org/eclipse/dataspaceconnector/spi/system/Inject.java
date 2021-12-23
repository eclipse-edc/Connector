package org.eclipse.dataspaceconnector.spi.system;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used on a {@link ServiceExtension}'s fields, so that they can be automatically set during
 * the extension load phase.
 * This annotation has no effect on any class other than a {@link ServiceExtension}.
 * <p>
 * do NOT @Inherited it, because that complicates things
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    boolean required() default true;
}
