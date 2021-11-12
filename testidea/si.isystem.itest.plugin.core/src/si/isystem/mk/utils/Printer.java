package si.isystem.mk.utils;


/**
 * This class simplifies printing to std out. It replaces verbose 
 * 'System.out.print*()' methods with shorter ones. Use static import for this 
 * class:
 * <pre>
 * import static si.isystem.mk.utils.Printer.*;
 * 
 * void main(String[args]) {
 *     println("The value of system property 'java.path' is: ", System.getProperty("java.path"));
 * }
 * </pre>
 *  
 * @author markok
 *
 */
public class Printer {

    /**
     * This utility class has only static methods and should not be 
     * instantiated. 
     */
    private Printer() {}

    /**
     * Prints args one by one with System.out.print().
     * 
     * Example: 
     * <pre>
     *    int i = 10;
     *    println("The value of 'i' is: ", i);
     * </pre>
     * 
     * @param args arguments to be printed - toString() method is called on each of them.
     */
    public static void print(Object ... args) {
        for (Object arg : args) {
            System.out.print(arg);
        }
    }
    
    
    /**
     * Prints args one by one with System.out.print(). Adds a new line at the end.
     * 
     * Example: 
     * <pre>
     *    int i = 10;
     *    println("The value of 'i' is: ", i);
     * </pre>
     * 
     * @param args arguments to be printed - toString() method is called on each of them.
     */
    public static void println(Object ... args) {
        for (Object arg : args) {
            System.out.print(arg);
        }
        System.out.println();
    }
    
    
    /**
     * The same as System.out.printf().
     * 
     * @see PrintStream.printf
     */
    public static void printf(String format, Object ... args) {
        System.out.printf(format, args);
    }
}
