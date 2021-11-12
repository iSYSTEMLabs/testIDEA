package si.isystem.commons.tuple;

import java.util.Objects;

import si.isystem.commons.utils.ISysMathUtils;

public class Trio<F, S, T> implements ITrio<F, S, T> 
{
    protected F m_first;
    protected S m_second;
    protected T m_third;

    public Trio(F first, S second, T third) {
        m_first = first;
        m_second = second;
        m_third = third;
    }

    @Override
    public F getFirst() {
        return m_first;
    }

    @Override
    public S getSecond() {
        return m_second;
    }

    @Override
    public T getThird() {
        return m_third;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(m_first, m_second, m_third);
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Trio)) {
            return false;
        }
        
        return  ISysMathUtils.isEqual(((Trio)obj).m_first, this.m_first)  &&
                ISysMathUtils.isEqual(((Trio)obj).m_second, this.m_second)  &&
                ISysMathUtils.isEqual(((Trio)obj).m_third, this.m_third);
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s, %s, %s)", 
                this.getClass().getSimpleName(),
                m_first, m_second, m_third);
    }
}
