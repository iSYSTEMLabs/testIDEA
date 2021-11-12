package si.isystem.commons.tuple;

public class MutablePair<L, R> extends Pair<L, R> implements IMutablePair<L, R> 
{
    public MutablePair() {
        super(null, null);
    }

    public MutablePair(L left, R right) {
        super(left, right);
    }

    @Override
    public void set(L left, R right) {
        setLeft(left);
        setRight(right);
    }

    @Override
    public void setLeft(L left) {
        m_left = left;
    }

    @Override
    public void setRight(R right) {
        m_right = right;
    }
}
