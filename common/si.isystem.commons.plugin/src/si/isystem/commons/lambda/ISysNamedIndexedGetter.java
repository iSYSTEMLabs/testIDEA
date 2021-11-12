package si.isystem.commons.lambda;

/**
 * This is just an adapter class for simpler anonymous class declaration
 */
public abstract class ISysNamedIndexedGetter<T> extends ISysNamed implements IIndexedGetter<T>, INamed {
    public ISysNamedIndexedGetter(String name) {
        super(name);
    }
}
