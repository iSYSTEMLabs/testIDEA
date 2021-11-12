package si.isystem.itest.preferences.controls;

import org.eclipse.swt.widgets.Composite;

import si.isystem.connect.CYAMLUtil;

/**
 * This filed editor can be used to edit sequences in single line text filed, 
 * where items are separated by commas, for example core IDs.
 * 
 * @author markok
 */
public class PrefListEditor extends PrefStringEditor {

    public PrefListEditor(int name, String labelText, 
                          int strategy, Composite parent, 
                          String layout,
                          String tooltip) {
        this(String.valueOf(name), labelText, strategy, parent, layout, tooltip);
    }
    
    public PrefListEditor(String name, String labelText, 
                          int strategy, Composite parent, 
                          String layout,
                          String tooltip) {
        
        super(name, labelText, strategy, parent, layout, tooltip);
    }
    
    
    @Override
    /**
     * Rules for core ID are the same as for testID.
     */
    protected boolean doCheckState() {
        String value = getStringValue();
        String [] items= value.split(",");
        for (String item : items) {
            item = item.trim();
            String error = CYAMLUtil.verifyTestId(item, "Invalid core ID!");
            if (!error.isEmpty()) {
                setErrorMessage(error);
                return false;
            }
        }
        return true;
    }
    
    
    @Override
    protected void doLoad() {
        String value = getPreferenceStore().getString(getPreferenceName());
        value = value.replace("\n", ", ");
        setStringValue(value);
        oldValue = value;
    }

    
    @Override
    protected void doLoadDefault() {
        String value = getPreferenceStore().getDefaultString(getPreferenceName());
        value = value.replace("\n", ", ");
        setStringValue(value);
        valueChanged();
    }


    @Override
    protected void doStore() {
        String value = getStringValue();
        value = value.replace(" ",  "");
        value = value.replace(",",  "\n");
        getPreferenceStore().setValue(getPreferenceName(), value);
    }
}
