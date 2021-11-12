package si.isystem.connect.data;

import java.util.EnumSet;
import java.util.List;

import si.isystem.connect.EOptions;
import si.isystem.connect.IDataDescriptor;
import si.isystem.connect.IDataDescriptor.EType;

/** 
 * This class wraps IVariableDescriptor, IElementDataDescriptor, and IDataDescriptor 
 * C++ objects.
 * 
 * (c) iSYSTEM AG, 2010
 */
public class JDataDescriptor {

    /* This class contains information about members of C/C++ enum types, including
     * name and value.
     */
    public static class EnumProperties {

        private final String m_name;
        private final int m_value;
        private final String m_description;
        private final EnumSet<EOptions> m_options;
        
        /**
         * Instantiates and initializes object with the given values. 
         *
         * @param name name of the enum constant
         * @param value value of the enum constant
         * @param description description of the enum constant
         * @param options see EOptions in i_DataDescriptor.h
         */
        public EnumProperties(String name,
                              int value,
                              String description,
                              EnumSet<EOptions> options) {
            super();
            m_name = name;
            m_value = value;
            m_description = description;
            m_options = options;
        }

        /** Returns enum name. */
        public String getName() {
            return m_name;
        }

        /** Returns enum value. */
        public int getValue() {
            return m_value;
        }

        /** Returns enum description. */
        public String getDescription() {
            return m_description;
        }

        /** Returns enum options. */
        public EnumSet<EOptions> getOptions() {
            return m_options;
        }
    }

    
    // fields from IVariableDescriptor and IElementDataDescriptor
    private final long m_addressAsCPtr; // if instance of this class is made of IElementDataDescriptor
                                        // this field should contain the address of structure member,
                                        // not offset. Calculate as parent.Data() + Offset()
    
    private final String m_name;
    private final int m_dimension; // if >= 0 it is an array
    private final String m_description;
    private final EnumSet<EOptions> m_options;
    
    // fields from IDataDescriptor
    private final IDataDescriptor.EType m_type;
    private final long m_size;
    private final List<EnumProperties> m_enumList;
    private final List<EnumProperties> m_enumIndexList;
    
    private final JDataDescriptor[] m_structChildren;

    /**
     * 
     * 
     * @param addressAsCPtr
     * @param name
     * @param dimension
     * @param description
     * @param options
     * @param type
     * @param size 
     * 
     * @param enumList list of enums (names and values) for enum types. If data
     * item is an array of enums, enumList describes possible values of array elements
     *     
     * @param enumIndexList if this list is not empty, array me indexed also by enum
     * names, not only integers, for example persons['Joe']. enumIndexList defines
     * mapping from string to enums in such case.
     * 
     * @param container
     * @param structChildren
     */
    public JDataDescriptor(long addressAsCPtr,
                           String name,
                           int dimension,
                           String description,
                           EnumSet<EOptions> options,
                           EType type,
                           long size,
                           List<EnumProperties> enumList,
                           List<EnumProperties> enumIndexList, 
                           IDataDescriptor.EContainer container,
                           JDataDescriptor[] structChildren) {
        super();
        m_addressAsCPtr = addressAsCPtr;
        m_name = name;
        m_dimension = dimension;
        m_description = description;
        m_options = options;
        m_type = type;
        m_size = size;
        m_enumList = enumList;
        m_enumIndexList = enumIndexList;
        m_structChildren = structChildren;
    }

    public long getAddressAsCPtr() {
        return m_addressAsCPtr;
    }

    public String getName() {
        return m_name;
    }

    public int getDimension() {
        return m_dimension;
    }

    public String getDescription() {
        return m_description;
    }

    public EnumSet<EOptions> getOptions() {
        return m_options;
    }

    public IDataDescriptor.EType getType() {
        return m_type;
    }

    public long getSize() {
        return m_size;
    }

    /** Returns list of enum values. */
    public List<EnumProperties> getEnumList() {
        return m_enumList;
    }

    /** Returns list of enum values. */
    public List<EnumProperties> getEnumIndexList() {
        return m_enumIndexList;
    }

    /** Returns children of this data descriptor. */
    public JDataDescriptor[] getChildren() {
        return m_structChildren;
    }
}
