package si.isystem.commons.tuple;

public class MutableTrio<F, S, T> extends Trio<F, S, T> implements IMutableTrio<F, S, T> 
{
    public MutableTrio() {
        super(null, null, null);
    }

    public MutableTrio(F first, S second, T third) {
        super(first, second, third);
    }

    @Override
    public void set(F first, S second, T third) {
        setFirst(first);
        setSecond(second);
        setThird(third);
    }

    @Override
    public void setFirst(F first) {
        m_first = first;
    }

    @Override
    public void setSecond(S second) {
        m_second = second;
    }

    @Override
    public void setThird(T third) {
        m_third = third;
    }
}
