package si.isystem.commons.tuple;

/**
 * Meant to be used as an item in collections where we have pairs of information 
 * that aren't directly related like keys and values in maps.
 * 
 * @author iztokv
 *
 * @param <L>
 * @param <R>
 */
public interface IPair<L, R> 
{
    L getLeft();
    R getRight();
}
