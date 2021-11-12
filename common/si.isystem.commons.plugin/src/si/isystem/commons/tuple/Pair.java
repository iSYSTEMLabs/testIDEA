package si.isystem.commons.tuple;

import java.util.Objects;

import si.isystem.commons.utils.ISysMathUtils;

public class Pair<L, R> implements IPair<L, R> 
{
    protected L m_left;
    protected R m_right;

    public Pair(L left, R right) {
        m_left = left;
        m_right = right;
    }
    
    @Override
    public L getLeft() {
        return m_left;
    }

    @Override
    public R getRight() {
        return m_right;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(m_left, m_right);
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
        if (!(obj instanceof Pair)) {
            return false;
        }
        
        Pair other = (Pair)obj;
        return  ISysMathUtils.isEqual(other.m_left, this.m_left)  &&
                ISysMathUtils.isEqual(other.m_right, this.m_right);
    }
    
    @Override
    public String toString() {
        return String.format("Pair(%s, %s)", m_left, m_right);
    }
}
