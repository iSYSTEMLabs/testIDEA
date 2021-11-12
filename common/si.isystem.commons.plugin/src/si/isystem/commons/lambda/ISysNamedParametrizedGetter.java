package si.isystem.commons.lambda;

/**
 * This is just an adapter class for simpler anonymous class declaration
 */
public abstract class ISysNamedParametrizedGetter extends ISysNamed implements ParametrizedGetter {
    public ISysNamedParametrizedGetter(String name) {
        super(name);
    }
}
