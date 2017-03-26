package java.util;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Some instantiations of this class prohibit null elements.
public interface SortedSet<E extends @NonNull Object> extends Set<E> {
  @SideEffectFree public abstract Comparator<? super E> comparator();
  @SideEffectFree public abstract SortedSet<E> subSet(E a1, E a2);
  @SideEffectFree public abstract SortedSet<E> headSet(E a1);
  @SideEffectFree public abstract SortedSet<E> tailSet(E a1);
  @SideEffectFree public abstract E first();
  @SideEffectFree public abstract E last();
}
