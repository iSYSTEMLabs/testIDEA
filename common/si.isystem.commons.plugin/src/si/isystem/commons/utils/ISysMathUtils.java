package si.isystem.commons.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collection;

import si.isystem.commons.tuple.Pair;

public class ISysMathUtils 
{
    public static int distance(int x1, int y1, int x2, int y2) {
        return (int)Math.sqrt(distanceSqr(x1, y1, x2, y2));
    }
    
    public static int distanceSqr(int x1, int y1, int x2, int y2) {
        return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
    }
    
    public static double distance(double x1, double y1, double x2, double y2) {
        return (int)Math.sqrt(distanceSqr(x1, y1, x2, y2));
    }
    
    public static double distanceSqr(double x1, double y1, double x2, double y2) {
        return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
    }
    
    //
    // Constraining given value to range [min, max]
    //
    
    public static byte constrainTo(byte value, byte min, byte max) {
        if (value <= min) {
            return min;
        }
        if (value >= max) {
            return max;
        }
        return value;
    }
    
    public static short constrainTo(short value, short min, short max) {
        if (value <= min) {
            return min;
        }
        if (value >= max) {
            return max;
        }
        return value;
    }
    
    public static int constrainTo(int value, int min, int max) {
        if (value <= min) {
            return min;
        }
        if (value >= max) {
            return max;
        }
        return value;
    }
    
    public static long constrainTo(long value, long min, long max) {
        if (value <= min) {
            return min;
        }
        if (value >= max) {
            return max;
        }
        return value;
    }
    
    public static float constrainTo(float value, float min, float max) {
        if (value <= min) {
            return min;
        }
        if (value >= max) {
            return max;
        }
        return value;
    }

    public static double constrainTo(double value, double min, double max) {
        if (value <= min) {
            return min;
        }
        if (value >= max) {
            return max;
        }
        return value;
    }

    public static BigDecimal constrainTo(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
    
    //
    // Is value between (min, max) - not inclusive min and max
    //
    
    public static boolean isBetween(short value, short min, short max) {
        return min < value  &&  value < max;
    }
    
    public static boolean isBetween(int value, int min, int max) {
        return min < value  &&  value < max;
    }
    
    public static boolean isBetween(long value, long min, long max) {
        return min < value  &&  value < max;
    }
    
    public static boolean isBetween(float value, float min, float max) {
        return min < value  &&  value < max;
    }
    
    public static boolean isBetween(double value, double min, double max) {
        return min < value  &&  value < max;
    }
    
    public static boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        return  min.compareTo(value) < 0  &&  value.compareTo(max) < 0;
    }
    
    //
    // Is value between (min, max) - inclusive min and max
    //
    
    public static boolean isBetweenOrEqual(short value, short min, short max) {
        return min <= value  &&  value <= max;
    }
    
    public static boolean isBetweenOrEqual(int value, int min, int max) {
        return min <= value  &&  value <= max;
    }
    
    public static boolean isBetweenOrEqual(long value, long min, long max) {
        return min <= value  &&  value <= max;
    }
    
    public static boolean isBetweenOrEqual(float value, float min, float max) {
        return min <= value  &&  value <= max;
    }
    
    public static boolean isBetweenOrEqual(double value, double min, double max) {
        return min <= value  &&  value <= max;
    }
    
    public static boolean isBetweenOrEqual(BigDecimal value, BigDecimal min, BigDecimal max) {
        return  min.compareTo(value) <= 0  &&  value.compareTo(max) <= 0;
    }
    
    //
    // Range in range
    //
    
    public static boolean contains(byte childMin, byte childMax, byte parentMin, byte parentMax) {
        return  parentMin <= childMin  &&  childMax <= parentMax;
    }
    
    public static boolean contains(short childMin, short childMax, short parentMin, short parentMax) {
        return  parentMin <= childMin  &&  childMax <= parentMax;
    }
    
    public static boolean contains(int childMin, int childMax, int parentMin, int parentMax) {
        return  parentMin <= childMin  &&  childMax <= parentMax;
    }
    
    public static boolean contains(long childMin, long childMax, long parentMin, long parentMax) {
        return  parentMin <= childMin  &&  childMax <= parentMax;
    }
    
    public static boolean contains(float childMin, float childMax, float parentMin, float parentMax) {
        return  parentMin <= childMin  &&  childMax <= parentMax;
    }
    
    public static boolean contains(double childMin, double childMax, double parentMin, double parentMax) {
        return  parentMin <= childMin  &&  childMax <= parentMax;
    }
    
    public static boolean contains(BigDecimal subRangeMin, BigDecimal subRangeMax, BigDecimal rangeMin, BigDecimal rangeMax) {
        return  rangeMin.compareTo(subRangeMin) <= 0  &&
                subRangeMax.compareTo(rangeMax) <= 0;
    }
    
    /**
     * Returns the size of the intersection between ranges [a1, a2] and [b1, b2].
     * @param a1
     * @param a2
     * @param b1
     * @param b2
     * @return
     */
    public static int intersectionSize(int aMin, int aMax, int bMin, int bMax) {
        if (aMin > aMax) {
            int t = aMax;
            aMax = aMin;
            aMin = t;
        }
        if (bMin > bMax) {
            int t = bMax;
            bMax = bMin;
            bMin = t;
        }
        int max = (aMax < bMax ? aMax : bMax); // Lesser of the two maximums
        int min = (aMin > bMin ? aMin : bMin); // Greater of the two minimums
        return (max - min);
    }

    public static long intersectionSize(long aMin, long aMax, long bMin, long bMax) {
        if (aMin > aMax) {
            long t = aMax;
            aMax = aMin;
            aMin = t;
        }
        if (bMin > bMax) {
            long t = bMax;
            bMax = bMin;
            bMin = t;
        }
        long max = (aMax < bMax ? aMax : bMax); // Lesser of the two maximums
        long min = (aMin > bMin ? aMin : bMin); // Greater of the two minimums
        return (max - min);
    }

    public static BigDecimal intersectionSize(BigDecimal aMin, BigDecimal aMax, BigDecimal bMin, BigDecimal bMax) {
        if (aMin.compareTo(aMax) > 0) {
            BigDecimal t = aMax;
            aMax = aMin;
            aMin = t;
        }
        if (bMin.compareTo(bMax) > 0) {
            BigDecimal t = bMax;
            bMax = bMin;
            bMin = t;
        }
        BigDecimal max = (aMax.compareTo(bMax) < 0 ? aMax : bMax); // Lesser of the two maximums
        BigDecimal min = (aMin.compareTo(bMin) > 0 ? aMin : bMin); // Greater of the two minimums
        return max.subtract(min);
    }

    /**
     * Returns a new sub array.
     * @param a
     * @param pos
     * @param len
     * @return
     */
    public static double[] getSubArray(double a[], int pos, int len) {
        double[] tmp = new double[len];
        System.arraycopy(a, pos, tmp, 0, len);
        return tmp;
    }

    /**
     * Used when a or b could both be null and we can't use a.equals(b) or b.equals(a).
     * @param a
     * @param b
     * @return
     */
    public static boolean isEqual(Object a, Object b) {
        if (a == null) {
            return (b == null);
        }
        return a.equals(b);
    }
    
    public static boolean isEmpty(Object[] a) {
        return a == null  ||  a.length == 0;
    }

    public static boolean isEmpty(Collection<?> c) {
        return c == null  ||  c.isEmpty();
    }

    public static BigInteger parseBigInt(String str) throws NumberFormatException {
        int radix = 10;
        String value = str;
        if (value.startsWith("0x")) {
            radix = 16;
            value = str.substring(2);
        }
        else if (value.startsWith("0c")) {
            radix = 8;
            value = str.substring(2);
        }
        else if (value.startsWith("0b")) {
            radix = 2;
            value = str.substring(2);
        }

        return new BigInteger(value, radix);
    }

    public static int sign(long l) {
        if (l < 0) {
            return -1;
        }
        if (l > 0) {
            return 1;
        }
        return 0;
    }

    public static int[] weightedArray(int sum, int count) {
        int[] weights = new int[count];
        return weightedArray(sum, weights);
    }

    public static int[] weightedArray(int sum, int[] weights) {
        int spaceLeft = sum;
        for (int i = 0; i < weights.length; i++) {
            int spaceTaken = spaceLeft / (weights.length - i);
            weights[i] = spaceTaken;
            spaceLeft -= spaceTaken;
        }
        return weights;
    }

    public static int sum(int[] values) {
        return sum(values, 0, values.length);
    }

    public static int sum(int[] values, int min, int max) {
        int sum = 0;
        for (int i = min; i < max; i++) {
            sum += values[i];
        }
        return sum;
    }

    public static double sum(double[] values) {
        return sum(values, 0, values.length);
    }

    public static double sum(double[] values, int min, int max) {
        double sum = 0;
        for (int i = min; i < max; i++) {
            sum += values[i];
        }
        return sum;
    }
    
    public static int min(int[] a) {
        int res = Integer.MAX_VALUE;
        for (int i = 0; i < a.length; i++) {
            res = (a[i] < res ? a[i] : res);
        }
        return res;
    }
    
    public static double min(double[] a) {
        double res = Integer.MAX_VALUE;
        for (int i = 0; i < a.length; i++) {
            res = (a[i] < res ? a[i] : res);
        }
        return res;
    }
    
    public static int max(int[] a) {
        int res = Integer.MIN_VALUE;
        for (int i = 0; i < a.length; i++) {
            res = (a[i] > res ? a[i] : res);
        }
        return res;
    }
    
    public static double max(double[] a) {
        double res = Integer.MIN_VALUE;
        for (int i = 0; i < a.length; i++) {
            res = (a[i] > res ? a[i] : res);
        }
        return res;
    }
    
    public static double[] asDoubleArray(int[] a) {
        double[] d = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            d[i] = a[i];
        }
        return d;
    }
    
    public static void subtract(int[] a, int v) {
        for (int i = 0; i < a.length; i++) {
            a[i] -=v;
        }
    }
    
    public static void subtract(double [] a, double v) {
        for (int i = 0; i < a.length; i++) {
            a[i] -=v;
        }
    }

    /**
     * Calculates the weighted values where their sum is the given sum.
     * @param weights
     * @param sum
     * @return
     */
    public static int[] weight(double weights[], int sum) {
        int[] sizes = new int[weights.length];
        
        double wSum = sum(weights);
        int space = sum;
        for (int i = 0; i < weights.length; i++) {
            sizes[i] = (int)Math.round(weights[i] / wSum * space);
            wSum -= weights[i];
            space -= sizes[i];
        }
        
        return sizes;
    }

    public static double toPowerOf2Floor(double x) {
        return (long) Math.pow(2, Math.max(Math.floor(Math.log(x)/Math.log(2)), 0));
    }

    public static double toPowerOf2Ceil(double x) {
        return (long) Math.pow(2, Math.max(Math.ceil(Math.log(x)/Math.log(2)), 0));
    }

    public static Pair<BigDecimal, BigDecimal> restrictRangeZoom(BigDecimal min, BigDecimal max, BigDecimal minRange) {
        BigDecimal range = max.subtract(min);
        if (range.compareTo(minRange) < 0) {
            BigDecimal two = new BigDecimal(2);
            BigDecimal c = min.add(max).divide(two);
            BigDecimal rangeDiv2 = minRange.divide(two);
            return new Pair<>(
                    c.subtract(rangeDiv2),
                    c.add(rangeDiv2));
        }
        else {
            return new Pair<>(min, max);
        }
    }
    
    public static final BigDecimal DOUBLE_MAX = new BigDecimal(Double.MAX_VALUE);
    public static final BigDecimal DOUBLE_MIN = new BigDecimal(Double.MIN_VALUE);
    
    public static final BigDecimal SIGNED_MAX = new BigDecimal(Long.MAX_VALUE);
    public static final BigDecimal SIGNED_MIN = new BigDecimal(Long.MIN_VALUE);
    
    public static final BigDecimal UNSIGNED_MAX = new BigDecimal(2).pow(64).subtract(BigDecimal.ONE);
    public static final BigDecimal UNSIGNED_MIN = BigDecimal.ZERO;

    public static BigDecimal toDoubleValue(BigDecimal value) 
    {
        if (value.compareTo(DOUBLE_MAX) > 0) {
            return DOUBLE_MAX;
        }
        if (value.compareTo(DOUBLE_MIN) < 0) {
            return DOUBLE_MIN;
        }
        return value;
    }

    public static BigDecimal toSignedIntegerValue(BigDecimal value, RoundingMode rm) {
        if (value.compareTo(SIGNED_MAX) > 0) {
            return SIGNED_MAX;
        }
        if (value.compareTo(SIGNED_MIN) < 0) {
            return SIGNED_MIN;
        }
        return value.setScale(0, rm);
    }

    public static BigDecimal toUnsignedIntegerValue(BigDecimal value, RoundingMode rm) {
        if (value.compareTo(UNSIGNED_MAX) > 0) {
            return UNSIGNED_MAX;
        }

        if (value.compareTo(UNSIGNED_MIN) < 0) {
            return UNSIGNED_MIN;
        }
        return value.setScale(0, rm);
    }
}
