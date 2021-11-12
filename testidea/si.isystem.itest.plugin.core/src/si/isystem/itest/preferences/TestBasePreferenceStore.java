package si.isystem.itest.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import si.isystem.connect.CSequenceAdapter;
import si.isystem.connect.CTestBase;
import si.isystem.connect.CTestObject;
import si.isystem.connect.CYAMLUtil;
import si.isystem.connect.CTestObject.ETestObjType;
import si.isystem.itest.model.TestSpecificationModel;

/**
 * This class replaces standard PreferenceStore class from RCP, when 
 * project settings are edited. Project settings, (menu 'File | Properties'),
 * are stored to CTestBase class, and this handling is done by this class. 
 *  
 * Values are retrieved based on their IDs - mapping keys are section IDs,
 * which are integers converted to strings so they can be used as mapping keys.
 * 
 * Lists should be passed as newline delimited strings ('\r\n' sequences are also 
 * tolerated), and are also returned as newline delimited strings. Newlines were 
 * chosen over commas, because commas can be part of file name.
 * 
 * @author markok
 */
public class TestBasePreferenceStore implements IPreferenceStore {

    private CTestBase m_testBase;
    private List<IPropertyChangeListener> m_propertuChangeListeners;

    private Map<String, String> m_defaults = new TreeMap<String, String>();
    
    // when caller sets new value, it is not set directly in the CTestBase object,
    // but it is buffered. This way it is possible to cancel operation in a dialog,
    // since the original object has not been modified yet.
    private Map<String, String> m_bufferedValues = new TreeMap<String, String>();
    private boolean m_isChanged = false;
    
    
    public TestBasePreferenceStore(CTestBase testBase) {
        m_testBase = testBase;
    }

    
    /**
     * Saves all items from m_bufferedValues the CTestBase. 
     */
    public void save() {
        if (!needsSaving()) {
            return;
        }
        
        for (Map.Entry<String, String> entry : m_bufferedValues.entrySet()) {

            int section = Integer.valueOf(entry.getKey()).intValue();
            String newValue = entry.getValue();
            String oldValue = null;
            
            if (m_testBase.getSectionType(section) == ETestObjType.EYAMLSeqence) {
                oldValue = m_testBase.getTagValue(section);
                newValue = newValue.replace("\r", "");
                String[] array = newValue.split("\n");
                CSequenceAdapter seq = new CSequenceAdapter(m_testBase, section, false);
                seq.resize(0);
                for (int i = 0; i < array.length; i++) {
                    String listItem = array[i].trim();
                    if (!listItem.isEmpty()) {
                        seq.add(-1, listItem);
                    }
                }
                
            } else if (m_testBase.getSectionType(section) == ETestObjType.ETestBaseList) {
                oldValue = m_testBase.getTestBaseList(section, true).toString();
                
                CYAMLUtil.parseTestBaseList(newValue, m_testBase, section);
                
            } else {
                oldValue = m_testBase.getTagValue(section);
                m_testBase.setTagValue(section, newValue);  
            }
            
            if (!oldValue.equals(newValue)) {
                TestSpecificationModel model = TestSpecificationModel.getActiveModel();
                if (model != null  &&  !model.isModelDirty()) {
                    model.setModelDirty(true);
                }
            }
        }
    }
    
    
    @Override
    public void addPropertyChangeListener(IPropertyChangeListener listener) {
        if (m_propertuChangeListeners == null) {
            m_propertuChangeListeners = new ArrayList<IPropertyChangeListener>();
        }
        m_propertuChangeListeners.add(listener);
    }


    @Override
    public boolean contains(String name) {
        return m_testBase.containsSection(Integer.valueOf(name).intValue());
    }


    @Override
    public void firePropertyChangeEvent(String name,
                                        Object oldValue,
                                        Object newValue) {
        if (m_propertuChangeListeners != null) {
            for (IPropertyChangeListener listener : m_propertuChangeListeners) {
                listener.propertyChange(new PropertyChangeEvent(this, name, oldValue, newValue));
            }
        }
    }


    @Override
    public boolean getDefaultBoolean(String name) {
        String value = m_defaults.get(name);
        if (value != null) {
            return Boolean.valueOf(value).booleanValue();
        }
        return false;  // false is the default for bools in itest
    }


    @Override
    public double getDefaultDouble(String name) {
        String value = m_defaults.get(name);
        if (value != null) {
            return Double.valueOf(value).doubleValue();
        }
        return 0;
    }


    @Override
    public float getDefaultFloat(String name) {
        String value = m_defaults.get(name);
        if (value != null) {
            return Float.valueOf(value).floatValue();
        }
        return 0;
    }


    @Override
    public int getDefaultInt(String name) {
        String value = m_defaults.get(name);
        if (value != null) {
            return Integer.valueOf(value).intValue();
        }
        return 0;
    }


    @Override
    public long getDefaultLong(String name) {
        String value = m_defaults.get(name);
        if (value != null) {
            return Long.valueOf(value).longValue();
        }
        return 0;
    }


    @Override
    public String getDefaultString(String name) {
        String value = m_defaults.get(name);
        if (value == null) {
            return "";
        }
        return value;
    }

    
    @Override
    public boolean getBoolean(String name) {
        String value = m_bufferedValues.get(name);
        if (value == null) {
            value = m_testBase.getBoolValueAsBool(Integer.valueOf(name).intValue()) ?
                    "true" : "false";
        }
        return Boolean.valueOf(value).booleanValue();
    }


    @Override
    public double getDouble(String name) {
        String value = m_bufferedValues.get(name);
        if (value == null) {
            value = m_testBase.getTagValue(Integer.valueOf(name).intValue());
        }
        return Double.valueOf(value).doubleValue();
    }


    @Override
    public float getFloat(String name) {
        String value = m_bufferedValues.get(name);
        if (value == null) {
            value = m_testBase.getTagValue(Integer.valueOf(name).intValue());
        }
        return Float.valueOf(value).floatValue();
    }


    @Override
    public int getInt(String name) {
        String value = m_bufferedValues.get(name);
        if (value == null) {
            value = m_testBase.getTagValue(Integer.valueOf(name).intValue());
        }
        if (value.isEmpty()) {
            return 0;
        }
        return Integer.valueOf(value).intValue();
    }


    @Override
    public long getLong(String name) {
        String value = m_bufferedValues.get(name);
        if (value == null) {
            value = m_testBase.getTagValue(Integer.valueOf(name).intValue());
        }
        return Long.valueOf(value).longValue();
    }


    @Override
    public String getString(String name) {
        String value = m_bufferedValues.get(name);
        if (value == null) {
            
            int section = Integer.valueOf(name).intValue();
            
            if (m_testBase.getSectionType(section) == CTestObject.ETestObjType.EYAMLSeqence) {
                
                CSequenceAdapter seq = new CSequenceAdapter(m_testBase, section, true);
                StringBuilder sb = new StringBuilder();
                for (int idx = 0; idx < seq.size(); idx++) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(seq.getValue(idx));
                }
                value = sb.toString();
                
            } else if (m_testBase.getSectionType(section) == CTestObject.ETestObjType.ETestBaseList) {
                value = m_testBase.getTestBaseList(section, true).toString();
            } else {
                value = m_testBase.getTagValue(section);
            }
        }
        return value;
    }

    public String getString(int name) {
        String value = m_bufferedValues.get(String.valueOf(name));
        if (value == null) {
            value = m_testBase.getTagValue(name);
        }
        return value;
    }

    @Override
    public boolean isDefault(String name) {
        // for all unspecified preferences in itest default value is empty string,
        // unless specified otherwise
        return getString(name).equals(getDefaultString(name));
    }


    @Override
    public boolean needsSaving() {
        return m_isChanged;
    }


    @Override
    public void putValue(String name, String value) {
        m_bufferedValues.put(name, value);
    }


    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener) {
        if (m_propertuChangeListeners != null) {
            m_propertuChangeListeners.remove(listener);
        }
    }


    @Override
    public void setDefault(String name, double value) {
        m_defaults.put(name, String.valueOf(value));
    }


    @Override
    public void setDefault(String name, float value) {
        m_defaults.put(name, String.valueOf(value));
    }


    @Override
    public void setDefault(String name, int value) {
        m_defaults.put(name, String.valueOf(value));
    }


    @Override
    public void setDefault(String name, long value) {
        m_defaults.put(name, String.valueOf(value));
    }


    @Override
    public void setDefault(String name, String defaultObject) {
        m_defaults.put(name, defaultObject);
    }


    @Override
    public void setDefault(String name, boolean value) {
        m_defaults.put(name, String.valueOf(value));
    }


    @Override
    public void setToDefault(String name) {
        m_bufferedValues.remove(name);
    }


    @Override
    public void setValue(String name, double value) {
        String newValue = String.valueOf(value);
        setValue(name, newValue);
    }


    @Override
    public void setValue(String name, float value) {
        String newValue = String.valueOf(value);
        setValue(name, newValue);
    }


    @Override
    public void setValue(String name, int value) {
        String newValue = String.valueOf(value);
        setValue(name, newValue);
    }
    

    @Override
    public void setValue(String name, long value) {
        String newValue = String.valueOf(value);
        setValue(name, newValue);
    }


    @Override
    public void setValue(String name, String newValue) {
        
        String oldValue = getString(name);
        
        m_bufferedValues.put(name, newValue);
        
        if (!newValue.equals(oldValue)) {
            firePropertyChangeEvent(name, oldValue, newValue);
            m_isChanged = true;
        }
    }


    @Override
    public void setValue(String name, boolean value) {
        String newValue = String.valueOf(value);
        setValue(name, newValue);
    }
    

    // the most often used methods are also implemented with ints as keys
    public void setValue(int section, boolean newValue) {
        setValue(String.valueOf(section), String.valueOf(newValue));
    }

    public void setValue(int section, int newValue) {
        setValue(String.valueOf(section), String.valueOf(newValue));
    }

    public void setValue(int section, String newValue) {
        setValue(String.valueOf(section), newValue);
    }
}
