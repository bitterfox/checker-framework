package dataflow.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Pure} is a method annotation that means both {@link
 * SideEffectFree} and {@link Deterministic}.  The more important of these,
 * when performing pluggable type-checking, is usually {@link
 * SideEffectFree}.
 *
 * @author Stefan Heule
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface Pure {
    /**
     * The type of purity.
     */
    public static enum Kind {
        /** The method has no visible side-effects. */
        SIDE_EFFECT_FREE,

        /**
         * The method returns exactly the same value when called in the same
         * environment.
         */
        DETERMINISTIC
    }
}
