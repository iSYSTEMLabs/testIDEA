package si.isystem.connect.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import si.isystem.connect.CAddress;

/** 
 * This class is immutable wrapper of CAddress. 
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JCAddress {

    private int m_area;
    private long m_address;
    private short m_process;

    /**
     * Instantiates object and initializes it with data from <sode>address</code>.
     * 
     * @param address C++ object containing address information
     */
    public JCAddress(CAddress address) {
        m_address = address.getM_aAddress();
        m_area = address.getM_iArea();
        m_process = address.getM_byProcess();
    }

    /**
     * Instantiates object and initializes it with the given parameters.
     * 
     * @param memArea memory area
     * @param address memory address
     */
    public JCAddress(int memArea, long address) {
        m_area = memArea;
        m_address = address;
        m_process = 0;
    }
    
    /** Returns address. */
    public long getAddress() {
        return m_address;
    }

    /** Returns memory area. */
    public int getArea() {
        return m_area;
    }

    /** Returns process. */
    public short getProcess() {
        return m_process;
    }
    
    
    private static Pattern simpleHexAddress = Pattern.compile("0[xX]([0-9A-Fa-f]+)");

    // Only hex addresses make sense as banked addresses
    // For decimal addresses add '|[bB][aA][nN][kK]:([0-9]+)'
    // Bank address consists of the following components:
    // 'bank:' - in upper or lower case
    // '0x' - hex prefix
    // [0-9A-Fa-f]{2} - two digit hex bank number
    // [0-9A-Fa-f]+ - at least one digit hex address
    private static Pattern bankAddress = 
        Pattern.compile("[bB][aA][nN][kK]:0[xX]([0-9A-Fa-f]{2})([0-9A-Fa-f]+)");


    /**
     * This factory method parses string address into internal representation.
     */
    public static JCAddress parseAddress(String addr) {
        try {
            Matcher matcher = simpleHexAddress.matcher(addr);

            if (matcher.matches()) {
                long addrL = Long.parseLong(matcher.group(1), 16);
                return new JCAddress(0, addrL);
            }

            matcher = bankAddress.matcher(addr);
            if (matcher.matches()) {
                int bank = Integer.parseInt(matcher.group(1), 16); 
                long addrL = Long.parseLong(matcher.group(2), 16);
                return new JCAddress(bank, addrL);
            }

            // it is a logical decimal address
            return new JCAddress(0, Long.parseLong(addr));
        } catch (Exception ex) {
            throw new NumberFormatException("Number format error! " + ex.getMessage() + 
                                            " Address should be either decimal number, " +
                                            "hex number (with '0x' prefix), or " +
                                            "given as bank address, for example:" +
                                            " 'bank:0xnnm, where 'nn' is hex bank " +
                                            "number and 'm' is 1 or more digit " +
                                            "hex address.");
        }
    }
    
    @Override
    public int hashCode()
    {
        return super.hashCode() ^ (m_area << 24) ^ (m_process << 20) ^ (int)m_address;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj)) {
            JCAddress addr = (JCAddress)obj;
            
            return addr.getAddress() == this.getAddress()  &&
                   addr.getArea() == this.getArea()  &&
                   addr.getProcess() == this.getProcess();
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return getClass().getName()+ ": area = " + m_area + ",  address = " + m_address + ")";
    }
}
