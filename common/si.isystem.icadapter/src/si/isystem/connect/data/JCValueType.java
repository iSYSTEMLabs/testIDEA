package si.isystem.connect.data;

import java.math.BigInteger;

import si.isystem.connect.CAddress;
import si.isystem.connect.CValueType;
import si.isystem.connect.SType;

/**
 * This class is immutable wrapper of CValueType. It converts C++ primitive types
 * to Java primitive types.
 * 
 * Values are always stored into value, which fits, and all higher range values.
 * For example, if value is of type signed long, then int is not assigned, but
 * long AND BigInterger are. If int is assigned, also long and BigInteger are.
 * Note that unsigned values do not fit into signed types of the same size. If
 * unsigned int is set, int is not set, only long and big integer are.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JCValueType {

    private final JSType m_type;
    private final int m_int;
    private final long m_long;
    private final float m_float;
    private final double m_double;
    private final JCAddress m_address;
    private final String m_string;
    private final BigInteger m_bigInt;
    private final boolean m_isError; // this flag was introduced
    // because it is sometimes better to provide error description, which is
    // written instead of value in Eclipse viewer, than throwing exception,
    // which may break refresh of valid items.  
    
    private final int m_hashCode;

    /**
     * Use this ctor, when error is returned instead of value from isystem.connect.
     */
    public JCValueType(String errorMsg) {
        m_type = null;
        m_int = 0;
        m_long = 0;
        m_float = 0;
        m_double = 0;
        m_address = null;
        m_string = errorMsg;
        m_bigInt = null;
        m_isError = true;
        m_hashCode = createHash();
    }


    public JCValueType(JSType type, int value) {
        if (type.getType() != SType.EType.tSigned.swigValue()  &&  
            type.getType() != SType.EType.tUnsigned.swigValue()) {
            throw new IllegalArgumentException("Invalid type for int argument: " + type.getType());
        }
        if (type.getType() == SType.EType.tUnsigned.swigValue()  &&  value < 0) {
            throw new IllegalArgumentException("Negative values are not allowed for " +
                    "unsigned type. Use wider type instead. Val = " + value);
        }
        m_type = type;
        m_int = value;
        m_long = value;
        m_float = 0;
        m_double = 0;
        m_address = null;
        m_string = null;
        m_bigInt = BigInteger.valueOf(value);
        m_isError = false;
        m_hashCode = createHash();
    }
    
    
    /**
     * 
     * @param type
     * @param value must be positive for unsigned type. Use JCValueType(type, string)
     * if 64 bit unsigned values must be specified.
     */
    public JCValueType(JSType type, long value) {
        if (type.getType() != SType.EType.tSigned.swigValue()  &&  
                type.getType() != SType.EType.tUnsigned.swigValue()) {
                throw new IllegalArgumentException("Invalid type for long argument: " + type.getType());
        }
        
        if (type.getType() == SType.EType.tUnsigned.swigValue()  &&  value < 0) {
            throw new IllegalArgumentException("Negative values are not allowed for " +
            		"unsigned type. Use JCValueType(type, string) instead. Val = " + value);
        }
        m_type = type;
        m_int = 0;
        m_long = value;
        m_float = 0;
        m_double = 0;
        m_address = null;
        m_string = null;
        m_bigInt = BigInteger.valueOf(value);
        m_isError = false;
        m_hashCode = createHash();
    }
    
    
    public JCValueType(JSType type, BigInteger value) {
        if (type.getType() != SType.EType.tSigned.swigValue()  &&  
                type.getType() != SType.EType.tUnsigned.swigValue()) {
            throw new IllegalArgumentException("Invalid type for BigInteger argument: " + 
                                               type.getType());
        }
        if (type.getType() == SType.EType.tUnsigned.swigValue()  &&  value.signum() < 0) {
            throw new IllegalArgumentException("Negative values are not allowed for " +
                    "unsigned type. Val = " + value);
        }
        m_type = type;
        m_int = 0;
        m_long = 0;
        m_float = 0;
        m_double = 0;
        m_address = null;
        m_string = null;
        m_bigInt = value;
        m_isError = false;
        m_hashCode = createHash();
    }
    
    
    public JCValueType(JSType type, float value) {
        if (type.getType() != SType.EType.tFloat.swigValue()) {
            throw new IllegalArgumentException("Invalid type for float argument: " + 
                                               type.getType());
        }
        m_type = type;
        m_int = 0;
        m_long = 0;
        m_float = value;
        m_double = value;
        m_address = null;
        m_string = null;
        m_bigInt = null;
        m_isError = false;
        m_hashCode = createHash();
    }
    
    
    public JCValueType(JSType type, double value) {
        if (type.getType() != SType.EType.tFloat.swigValue()) {
            throw new IllegalArgumentException("Invalid type for double argument: " + 
                                               type.getType());
        }
        m_type = type;
        m_int = 0;
        m_long = 0;
        m_float = 0;
        m_double = value;
        m_address = null;
        m_string = null;
        m_bigInt = null;
        m_isError = false;
        m_hashCode = createHash();
    }
    
    
    /**
     * If type == SType.EType.tCompound, then value is stored as literal string.
     * If 'type' is set to numeric type, then it is converted to numeric value.
     * 
     * @param type
     * @param value
     */
    public JCValueType(JSType type, String value) {
        m_type = type;
        m_string = value;
        m_isError = false;
        
        switch (type.getTypeAsEnum()) {
        case tSigned:
            m_float = 0;
            m_double = 0;
            m_address = null;

            if (m_type.getBitSize() < 33) {
                m_int = Integer.parseInt(value);
                m_long = Long.parseLong(value); 
                m_bigInt = BigInteger.valueOf(m_long);
            } else if (m_type.getBitSize() < 65) {
                m_int = 0;
                m_long = Long.parseLong(value); 
                m_bigInt = BigInteger.valueOf(m_long);
            } else {
                m_int = 0;
                m_long = 0;
                if (value.startsWith("0x") || value.startsWith("0X")) {
                    m_bigInt = new BigInteger(value.substring(2), 16);
                } else {
                    m_bigInt = new BigInteger(value);
                }
            }
            break;
        case tUnsigned:
            m_float = 0;
            m_double = 0;
            m_address = null;

            if (m_type.getBitSize() < 32) {
                if (value.startsWith("0x") || value.startsWith("0X")) {
                    m_int = Integer.parseInt(value.substring(2), 16);
                    m_long = Long.parseLong(value.substring(2), 16);
                } else {
                    m_int = Integer.parseInt(value);
                    m_long = Long.parseLong(value);
                }
                m_bigInt = BigInteger.valueOf(m_long);
            } else if (m_type.getBitSize() < 64) {
                m_int = 0;
                if (value.startsWith("0x") || value.startsWith("0X")) {
                    m_long = Long.parseLong(value.substring(2), 16);
                } else {
                    m_long = Long.parseLong(value);
                }
                m_bigInt = BigInteger.valueOf(m_long);
            } else {
                m_int = 0;
                m_long = 0;
                if (value.startsWith("0x") || value.startsWith("0X")) {
                    m_bigInt = new BigInteger(value.substring(2), 16);
                } else {
                    m_bigInt = new BigInteger(value);
                }
            } 
            break;
        case tAddress:
            m_int = 0;
            m_long = 0;
            m_bigInt = BigInteger.ZERO;
            m_float = 0;
            m_double = 0;
            m_address = JCAddress.parseAddress(value);
            break;
        case tFloat:
            m_int = 0;
            m_long = 0;
            m_bigInt = null;
            m_address = null;
            if (m_type.getBitSize() < 33) {
                m_float = Float.parseFloat(value);
                m_double = m_float;
            } else if (m_type.getBitSize() < 65) {
                m_float = 0;
                m_double = Double.parseDouble(value);
            } else {
                throw new IllegalStateException("Invalid bit size for floats or doubles: " + 
                                                m_type.getBitSize());
            }
            break;
        case tCompound:
            m_int = 0;
            m_long = 0;
            m_bigInt = null;
            m_float = 0;
            m_double = 0;
            m_address = null;
            // m_string = value; already set
            break;
        default:
            throw new IllegalArgumentException("Invalid type for string argument: " + 
                                               type.getTypeAsEnum());
        }
        m_hashCode = createHash();
    }
    
    
    public JCValueType(JSType type, JCAddress value) {
        if (type.getType() != SType.EType.tAddress.swigValue()) {
            throw new IllegalArgumentException("Invalid type for address argument: " + 
                                               type.getType());
        }
        m_type = type;
        m_int = 0;
        m_long = 0;
        m_float = 0;
        m_double = 0;
        m_address = value;
        m_string = null;
        m_bigInt = null;
        m_isError = false;
        m_hashCode = createHash();
    }
    
    
    public JCValueType(CValueType valueType) {
        
        m_string = valueType.getResult().trim();

        m_isError = valueType.isError();
        if (m_isError) {
            m_type = null;
            m_int = 0;
            m_long = 0;
            m_float = 0;
            m_double = 0;
            m_address = null;
            m_bigInt = null;
            m_hashCode = createHash();
            return;
        }

        m_type = new JSType(valueType.getType());
        
        SType.EType eType = SType.EType.swigToEnum(m_type.getType());
        
        switch (eType) {
        case tSigned:
            m_float = 0;
            m_double = 0;
            m_address = null;
            if (m_type.getBitSize() < 33) {
                m_int = valueType.getInt();
                m_long = valueType.getLong(); 
                m_bigInt = BigInteger.valueOf(valueType.getLong());
            } else if (m_type.getBitSize() < 65) {
                m_int = 0;
                m_long = valueType.getLong(); 
                m_bigInt = BigInteger.valueOf(valueType.getLong());
            } else {
                m_int = 0;
                m_long = 0;
                if (m_string.startsWith("0x") || m_string.startsWith("0X")) {
                    m_bigInt = new BigInteger(m_string.substring(2), 16);
                } else {
                    m_bigInt = new BigInteger(m_string);
                }
            }
            break;
        case tUnsigned:
            m_float = 0;
            m_double = 0;
            m_address = null;
            if (m_type.getBitSize() < 32) {
                m_int = valueType.getInt();
                m_long = valueType.getLong(); 
                m_bigInt = BigInteger.valueOf(valueType.getLong());
            } else if (m_type.getBitSize() < 64) {
                m_int = 0;
                m_long = valueType.getLong(); 
                m_bigInt = BigInteger.valueOf(valueType.getLong());
            } else {
                m_int = 0;
                m_long = 0;
                if (m_string.startsWith("0x") || m_string.startsWith("0X")) {
                    m_bigInt = new BigInteger(m_string.substring(2), 16);
                } else {
                    m_bigInt = new BigInteger(m_string);
                }
            } 
            break;
        case tFloat:
            m_int = 0;
            m_long = 0;
            m_bigInt = BigInteger.ZERO;
            m_address = null;
            if (m_type.getBitSize() < 33) {
                m_float = valueType.getFloat();
                m_double = valueType.getFloat();
            } else if (m_type.getBitSize() < 65) {
                m_float = 0;
                m_double = valueType.getDouble();
            } else {
                throw new IllegalStateException("Invalid bit size for floats or doubles: " + 
                                                m_type.getBitSize());
            }
            break;
        case tAddress:
            m_int = 0;
            m_long = 0;
            m_bigInt = BigInteger.ZERO;
            m_float = 0;
            m_double = 0;
            m_address = new JCAddress(valueType.getAddress());
            break;
        case tCompound:
            m_int = 0;
            m_long = 0;
            m_bigInt = BigInteger.ZERO;
            m_float = 0;
            m_double = 0;
            m_address = null;
            // intentionally left empty - CValueType and SValue do not have storage for this  
            break;
        default:
            throw new IllegalStateException("Invalid type: " + eType);
        }
        m_hashCode = createHash();
    }
    
    private int createHash()
    {
        int hash = 0;
        hash ^= (m_type != null ? m_type.hashCode() : 0);
        hash ^= m_int + m_long;
        hash ^= Float.floatToIntBits(m_float) + (int)Double.doubleToLongBits(m_double);
        hash ^= (m_address != null ? m_address.hashCode() : 0);
        hash ^= (m_string != null ? m_string.hashCode() : 0);
        hash ^= (m_bigInt != null ? m_bigInt.hashCode() : 0);
        hash ^= (m_isError ? 1 : 0);

        return hash;
    }

    
    public JSType getType() {
        return m_type;
    }

    public int getInt() {
        return m_int;
    }

    public long getLong() {
        return m_long;
    }

    public float getFloat() {
        return m_float;
    }

    public double getDouble() {
        return m_double;
    }

    public JCAddress getAddress() {
        return m_address;
    }

    public String getString() {
        return m_string;
    }

    public BigInteger getBigInt() {
        return m_bigInt;
    }

    public boolean isAddressType() {
        return m_type.getType() == SType.EType.tAddress.swigValue();
    }

    public boolean isError() {
        return m_isError;
    }
    
    /**
     * This method converts value back to CValueType. Use this method only when 
     * you need CValueType as parameter to isystem.connect method call.
     */
    public CValueType getCValueType() {
        
        SType type = new SType(); 
        type.setM_byBitSize(m_type.getBitSize());
        type.setM_byType(m_type.getType());
        
        SType.EType eType = SType.EType.swigToEnum(m_type.getType());

        CValueType value = null;

        switch (eType) {
        case tSigned:
            if (m_type.getBitSize() < 33) {
                value = new CValueType(type, m_int);
            } else if (m_type.getBitSize() < 65) {
                value = new CValueType(type, m_long);
            } else {
                throw new IllegalStateException("Invalid bit size: " + m_type.getBitSize());
            }
            break;
        case tUnsigned:
            /*if (m_type.getBitSize() < 32) { // int can hold only 31 bit unsigned values
                value = new CValueType(type, m_int);
            } else if (m_type.getBitSize() < 64) {
                value = new CValueType(type, m_long);
            } else */ if (m_type.getBitSize() <= 64) {
                value = new CValueType(type, m_bigInt.toString());
            } else {
                throw new IllegalStateException("Invalid bit size: " + m_type.getBitSize());
            }
            break;
        case tFloat:
            if (m_type.getBitSize() == 32) {
                value = new CValueType(type, m_float);
            } else if (m_type.getBitSize() == 64) {
                value = new CValueType(type, m_double);
            } else {
                throw new IllegalStateException("Invalid bit size for float: " + m_type.getBitSize());
            }
            break;
        case tAddress:
            CAddress address = new CAddress();
            address.setM_iArea((short)m_address.getArea());
            address.setM_aAddress(m_address.getAddress());
            address.setM_byProcess(m_address.getProcess());
            value = new CValueType(type, address);
            break;
        case tCompound:
            value = new CValueType(type, m_string);
            break;
        default:
            break;
        } 
        
        return value;
    }
    
    @Override
    public int hashCode()
    {
        return m_hashCode;
    }
}
