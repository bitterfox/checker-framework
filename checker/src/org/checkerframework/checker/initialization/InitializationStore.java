package org.checkerframework.checker.initialization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ClassName;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.ThisReference;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;

/**
 * A store that extends {@code CFAbstractStore} and additionally tracks which fields of the 'self'
 * reference have been initialized.
 *
 * @author Stefan Heule
 * @see InitializationTransfer
 */
public class InitializationStore<V extends CFAbstractValue<V>, S extends InitializationStore<V, S>>
        extends CFAbstractStore<V, S> {

    /** The set of fields that are initialized. */
    protected final Set<VariableElement> initializedFields;
    /** The set of fields that have 'invariant' annotation. */
    protected final Map<FieldAccess, V> invariantFields;

    public InitializationStore(CFAbstractAnalysis<V, S, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        initializedFields = new HashSet<>();
        invariantFields = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the receiver is a field, and has an invariant annotation, then it can be considered
     * initialized.
     */
    @Override
    public void insertValue(Receiver r, V value) {
        if (value == null) {
            // No need to insert a null abstract value because it represents
            // top and top is also the default value.
            return;
        }

        super.insertValue(r, value);

        InitializationAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory =
                (InitializationAnnotatedTypeFactory<?, ?, ?, ?>) analysis.getTypeFactory();
        QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
        AnnotationMirror invariantAnno = atypeFactory.getFieldInvariantAnnotation();
        for (AnnotationMirror a : value.getAnnotations()) {
            if (qualifierHierarchy.isSubtype(a, invariantAnno)) {
                if (r instanceof FieldAccess) {
                    FieldAccess fa = (FieldAccess) r;
                    // Remember fields that have the 'invariant' annotation in the store.
                    if (!invariantFields.containsKey(fa)) {
                        invariantFields.put(
                                fa,
                                analysis.createSingleAnnotationValue(invariantAnno, r.getType()));
                    }
                    if (fa.getReceiver() instanceof ThisReference
                            || fa.getReceiver() instanceof ClassName) {
                        addInitializedField(fa.getField());
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Additionally, the {@link InitializationStore} keeps all field values for fields that have
     * the 'invariant' annotation.
     */
    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory atypeFactory, V val) {
        AnnotationMirror fieldInvariantAnnotation =
                ((InitializationAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
                        .getFieldInvariantAnnotation();

        for (FieldAccess invariantField : invariantFields.keySet()) {
            fieldValues.remove(invariantField);
        }

        super.updateForMethodCall(n, atypeFactory, val);

        // Add invariant annotation again.
        fieldValues.putAll(invariantFields);
    }

    /** A copy constructor. */
    public InitializationStore(S other) {
        super(other);
        initializedFields = new HashSet<>(other.initializedFields);
        invariantFields = new HashMap<>(other.invariantFields);
    }

    /**
     * Mark the field identified by the element {@code field} as initialized (if it belongs to the
     * current class, or is static (in which case there is no aliasing issue and we can just add all
     * static fields).
     */
    public void addInitializedField(FieldAccess field) {
        boolean fieldOnThisReference = field.getReceiver() instanceof ThisReference;
        boolean staticField = field.isStatic();
        if (fieldOnThisReference || staticField) {
            initializedFields.add(field.getField());
        }
    }

    /**
     * Mark the field identified by the element {@code f} as initialized (the caller needs to ensure
     * that the field belongs to the current class, or is a static field).
     */
    public void addInitializedField(VariableElement f) {
        initializedFields.add(f);
    }

    /** Is the field identified by the element {@code f} initialized? */
    public boolean isFieldInitialized(Element f) {
        return initializedFields.contains(f);
    }

    @Override
    protected boolean supersetOf(CFAbstractStore<V, S> o) {
        if (!(o instanceof InitializationStore)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        S other = (S) o;
        for (Element field : other.initializedFields) {
            if (!initializedFields.contains(field)) {
                return false;
            }
        }
        return super.supersetOf(other);
    }

    @Override
    public S leastUpperBound(S other) {
        Map<FlowExpressions.FieldAccess, V> oldFieldValues = fieldValues;
        fieldValues = new HashMap<>(fieldValues);
        for (FieldAccess invariantField : invariantFields.keySet()) {
            fieldValues.remove(invariantField);
        }
        Map<FlowExpressions.FieldAccess, V> oldOtherFieldValues = other.fieldValues;
        other.fieldValues = new HashMap<>(other.fieldValues);
        for (FieldAccess invariantField : other.invariantFields.keySet()) {
            other.fieldValues.remove(invariantField);
        }
        S result = super.leastUpperBound(other);
        fieldValues = oldFieldValues;
        other.fieldValues = oldOtherFieldValues;

        // Set intersection for initializedFields.
        result.initializedFields.addAll(other.initializedFields);
        result.initializedFields.retainAll(initializedFields);

        // Set intersection for invariantFields.
        result.invariantFields.putAll(other.invariantFields);
        result.invariantFields.putAll(invariantFields);
        //        result.invariantFields.retainAll(invariantFields);

        // Add invariant annotation again.
        //        for (Entry<FieldAccess, V> invariantField : invariantFields.entrySet()) {
        ////            result.insertValue(invariantField.getKey(), invariantField.getValue());
        //        }
        //        for (Entry<FieldAccess, V> invariantField : other.invariantFields.entrySet()) {
        ////            result.insertValue(invariantField.getKey(), invariantField.getValue());
        //        }
        result.fieldValues.putAll(invariantFields);
        result.fieldValues.putAll(other.invariantFields);

        return result;
    }

    @Override
    protected void internalVisualize(CFGVisualizer<V, S, ?> viz) {
        super.internalVisualize(viz);
        viz.visualizeStoreKeyVal("initialized fields", initializedFields);
    }

    public Map<FieldAccess, V> getFieldValues() {
        return fieldValues;
    }

    public CFAbstractAnalysis<V, S, ?> getAnalysis() {
        return analysis;
    }
}
