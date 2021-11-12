package si.isystem.mk.utils;

import java.text.NumberFormat;
import java.text.ParseException;

/**
 * This class can be used to convert integers with many digits to something more
 * readable. For example: 10.000.000.000 is much more readable than 10000000000,
 * and "12 ms 120 us" is much better than 12000120000.  
 * 
 * @author markok
 *
 */
public class ReadableNumberConverter {

    /**
     * This utility class has only static methods and should not be 
     * instantiated. 
     */
    private ReadableNumberConverter() {}
    
    /**
     * Converts number to readable string, for example: 10000000 -> 10.000.000
     * <br>Separator depends on locale.
     * @param num
     * @return
     */
    public static String num2str(long num) {
        NumberFormat format = NumberFormat.getIntegerInstance();
        return format.format(num);
    }

    
    /**
     * Converts readable string to number, for example: 10.000.000 -> 10000000
     * <br>Separator depends on locale.
     * @param num
     * @return
     */
    public static long str2num(String num) throws ParseException {
        NumberFormat format = NumberFormat.getIntegerInstance();
        return format.parse(num).longValue(); 
    }

    
    /**
     * Converts number given as nanoseconds to time, for example: 10000000 -> 10 ms 0 us 0 ns.
     * 
     * @param num
     * @return
     */
    public static String num2Time(long num) {
        StringBuilder sb = new StringBuilder();
        final String units[] = {" ns  ", " us  ", " ms  ", " s  ", 
                                " ", " ", " ", " ", " "}; // long has at most 19 digits 
        int idx = 0;

        // handle times shorter than 1 second
        do {
            long prevVal = num;
            num  /= 1000;
            sb.insert(0, units[idx++]);
            sb.insert(0, prevVal - num * 1000);
        } while (num > 0  &&  idx < 3);

        // handle times longer than 1 second
        while ((num / 1000) > 0) {
            long prevVal = num;
            num  /= 1000;
            sb.insert(0, units[idx++]);
            sb.insert(0, String.format("%03d", prevVal - num * 1000));
        }

        // We don't want zeroes before the first nonzero digit
        if (num > 0) {
            long prevVal = num;
            num  /= 1000;
            sb.insert(0, units[idx++]);
            sb.insert(0, prevVal - num * 1000);
        }
        
        return sb.toString();
    }

    
    /**
     * Not finished yet - currently parses numbers only. In the future it will
     * be expanded to parse strings like "3s 120ms1us" and return time in nanoseconds.
     * 
     * @param time
     * @return
     */
    public static long time2Num(String time) {
        return Long.parseLong(time);
    }

}
