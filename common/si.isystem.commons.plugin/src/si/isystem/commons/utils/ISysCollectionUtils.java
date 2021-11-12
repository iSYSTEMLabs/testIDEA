package si.isystem.commons.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import si.isystem.commons.lambda.IParametrizedGetter;

public class ISysCollectionUtils {

    public static int size(Collection<?> collection) {
        if (collection == null) {
            return 0;
        }
        return collection.size();
    }
    
    public static int size(byte[] a) {
        if (a == null) {
            return 0;
        }
        return a.length;
    }
    
    public static int size(short[] a) {
        if (a == null) {
            return 0;
        }
        return a.length;
    }
    
    public static int size(int[] a) {
        if (a == null) {
            return 0;
        }
        return a.length;
    }
    
    public static int size(long[] a) {
        if (a == null) {
            return 0;
        }
        return a.length;
    }
    
    public static int size(float[] a) {
        if (a == null) {
            return 0;
        }
        return a.length;
    }
    
    public static int size(double[] a) {
        if (a == null) {
            return 0;
        }
        return a.length;
    }
    
    public static int size(Object[] a) {
        if (a == null) {
            return 0;
        }
        return a.length;
    }

    /**
     * Sets the <code>value</code> to the <code>idx</code> place in the <code>array</code>. the array is extended if too short
     * and the new extended part of the array is filled with <code>defaultValue</code>
     * @param array
     * @param idx
     * @param value
     * @param defaultValue
     * @return
     */
    public static int[] set(int[] array, int idx, int value, int defaultValue) {
        if (array == null) {
            array = new int[idx+1];
        }
        if (array.length <= idx) {
            int[] tmp = new int[idx+1];
            Arrays.fill(tmp, defaultValue);
            System.arraycopy(array, 0, tmp, 0, array.length);
            array = tmp;
        }
        
        array[idx] = value;
        return array;
    }

    /**
     * Get index of an element inside of an unsorted array
     * @param array
     * @param element
     * @return
     */
    public static int getIndexOf(Object[] array, Object element) {
        if (array == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (ISysMathUtils.isEqual(array[i], element)) {
                return i;
            }
        }
        return -1;
    }

    public static int getLastIndexOf(Object[] array, Object element) {
        if (array == null) {
            return -1;
        }
        for (int i = array.length-1; i >= 0; i--) {
            if (ISysMathUtils.isEqual(array[i], element)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Does arr array start with the elements in start array.
     * @param arr
     * @param start
     * @return
     */
    public static boolean startsWith(Object[] arr, Object[] start) {
        if (start == null) {
            return true;
        }
        if (arr == null) {
            return false;
        }
        if (arr.length < start.length) {
            return false;
        }
        
        for (int i = 0; i < start.length; i++) {
            if (arr[i] != start[i]) {
                return false;
            }
        }
        return true;
    }

    public static Object[] concat(Object[] a, Object[] b) {
        final int aLen = (a != null ? a.length : 0);
        final int bLen = (b != null ? b.length : 0);
        Object[] res = new Object[aLen + bLen];
        if (a != null) {
            System.arraycopy(a, 0, res, 0, aLen);
        }
        if (b != null) {
            System.arraycopy(b, 0, res, aLen, bLen);
        }
        return res;
    }

    public static String[] concat(String[] a, String[] b) {
        final int aLen = (a != null ? a.length : 0);
        final int bLen = (b != null ? b.length : 0);
        String[] res = new String[aLen + bLen];
        if (a != null) {
            System.arraycopy(a, 0, res, 0, aLen);
        }
        if (b != null) {
            System.arraycopy(b, 0, res, aLen, bLen);
        }
        return res;
    }

    public static Object[] append(Object[] a, Object newObj) {
        if (a == null  ||  a.length == 0) {
            return new Object[] { newObj };
        }
        
        Object[] res = new Object[a.length+1];
        System.arraycopy(a, 0, res, 0, a.length);
        res[a.length] = newObj;
        return res;
    }

    public static String[] append(String[] a, String newObj) {
        if (a == null  ||  a.length == 0) {
            return new String[] { newObj };
        }
        
        String[] res = new String[a.length+1];
        System.arraycopy(a, 0, res, 0, a.length);
        res[a.length] = newObj;
        return res;
    }

    public static String[] prepend(String newStr, String[] a) {
        if (a == null  ||  a.length == 0) {
            return new String[] { newStr };
        }
        
        String[] res = new String[a.length+1];
        res[0] = newStr;
        System.arraycopy(a, 0, res, 1, a.length);
        return res;
    }
    
    public static Object[] resize(Object[] a, int newLength) {
        if (newLength <= 0) {
            return new Object[0];
        }
        Object[] res = new Object[newLength];
        if (a != null) {
            System.arraycopy(a, 0, res, 0, Math.min(a.length, newLength));
        }
        return res;
    }
    
    public static <T> String[] toStringArray(T[] a, IParametrizedGetter<T, String> getter) {
        if (a == null) {
            return new String[0];
        }
        String[] res = new String[a.length];
        for (int i = 0; i < a.length; i++) {
            T obj = a[i];
            if (obj != null) {
                res[i] = getter.get(obj);
            }
        }
        return res;
    }
    
    /**
     * Removes items which agree with the equalizer method.
     * @param coll
     * @param equalizer
     */
    public static <T> void removeItems(Collection<T> coll, IParametrizedGetter<T, Boolean> equalizer) {
        for (Iterator<T> it = coll.iterator(); it.hasNext(); ) {
            T item = it.next();
            if (equalizer.get(item)) {
                it.remove();
            }
        }
    }

    /**
     * Keeps the items which agree with the equalizer method.
     * @param coll
     * @param equalizer
     */
    public static <T> void retainItems(Collection<T> coll, IParametrizedGetter<T, Boolean> equalizer) {
        for (Iterator<T> it = coll.iterator(); it.hasNext(); ) {
            T item = it.next();
            if (!equalizer.get(item)) {
                it.remove();
            }
        }
    }
    
    /**
     * Parses a string presentation of an int[] array. e.g. "[1, 2, 3, 4, 5]"
     * @param str
     * @return
     */
    public static int[] parseIntArray(String str) {
        if (str == null  || str.length() == 0  ||  str.equals("null")) {
            return null;
        }
        
        List<Integer> ints = new ArrayList<>();
        str = str.substring(str.indexOf('[') + 1, str.lastIndexOf(']'));
        StringTokenizer st = new StringTokenizer(str, ",");
        while (st.hasMoreTokens()) {
            String t = st.nextToken().trim();
            ints.add(Integer.parseInt(t));
        }
        
        int[] res = new int[ints.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = ints.get(i);
        }
        return res;
    }

    public static int indexOf(Object[] a, Object o) {
        for (int i = 0; i < a.length; i++) {
            if (ISysMathUtils.isEqual(a[i], o)) {
                return i;
            }
        }
        return -1;
    }
    
    //
    // Object collection to primitive array transformation.
    //
    
    public static byte[] toByteArray(Collection<Byte> collection) {
        int index = 0;
        byte[] array = new byte[collection.size()];
        for (Byte element : collection) {
            array[index] = element.byteValue();
            index++;
        }
        return array;
    }
    
    public static short[] toShortArray(Collection<Short> collection) {
        int index = 0;
        short[] array = new short[collection.size()];
        for (Short element : collection) {
            array[index] = element.shortValue();
            index++;
        }
        return array;
    }
    
    public static int[] toIntArray(Collection<Integer> collection) {
        int index = 0;
        int[] array = new int[collection.size()];
        for (Integer element : collection) {
            array[index] = element.intValue();
            index++;
        }
        return array;
    }
    
    public static long[] toLongArray(Collection<Long> collection) {
        int index = 0;
        long[] array = new long[collection.size()];
        for (Long element : collection) {
            array[index] = element.longValue();
            index++;
        }
        return array;
    }
}
