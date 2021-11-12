package si.isystem.commons.tuple;

public interface IMutableTrio<F, S, T> extends ITrio<F, S, T> 
{
    void set(F first, S second, T third);
    void setFirst(F first);
    void setSecond(S second);
    void setThird(T third);
}
