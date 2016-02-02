package org.checkerframework.checker.initialization.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * {@link FBCBottom} marks the bottom of the Freedom Before Commitment type
 * hierarchy.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 * @author Stefan Heule
 */
@SubtypeOf({ UnderInitialization.class, Initialized.class })
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE})
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
public @interface FBCBottom {
}
