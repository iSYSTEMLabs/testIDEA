package si.isystem.commons.tuple;

public interface IMutablePair<L, R> extends IPair<L, R> 
{
    void set(L left, R right);
    void setLeft(L left);
    void setRight(R right);
}
