package si.isystem.commons.tuple;

/**
 * Meant to be used as an item in collections where we have three items of information 
 * that aren't directly related like keys and values in maps.

 * @author iztokv
 *
 * @param <F>
 * @param <S>
 * @param <T>
 */
public interface ITrio<F, S, T> 
{
    F getFirst();
    S getSecond();
    T getThird();
}
