package si.isystem.commons.lambda;

/**
 * This is just an adapter class for simpler anonymous class declaration
 */
public abstract class ISysNamedGetter<T> extends ISysNamed implements IGetter<T>, INamed {
    public ISysNamedGetter(String name) {
        super(name);
    }
}
