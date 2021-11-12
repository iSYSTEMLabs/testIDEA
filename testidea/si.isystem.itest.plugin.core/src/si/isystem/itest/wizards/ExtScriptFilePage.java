package si.isystem.itest.wizards;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import si.isystem.itest.common.ISysPathFileUtils;
import si.isystem.itest.main.Activator;
import si.isystem.ui.utils.FileNameBrowser;
import si.isystem.ui.utils.KGUIBuilder;
import si.isystem.ui.utils.UiTools;

public class ExtScriptFilePage extends WizardPage {

    private static final String PREFS_IS_OPEN_EDITOR = "itestScriptExt.isOpenEditor";

    private static final String PREFS_PY_EDITOR_PATH = "itest.pyEditorPath";
    
    private String m_extensionClassName = "";
    private String m_extensionModuleName = "";
    private FileNameBrowser m_scriptFileNameBrowser;
    private Text m_classNameTxt;
    private Button m_isOverwriteRb;
    private Button m_isOpenScriptInEditorCb;

    private FileNameBrowser m_editorPath;

    private Button m_isAppendRb;

    private KGUIBuilder m_overwriteGrp;

    protected ExtScriptFilePage(String extensionModuleAndClass) {
        super("Script file");
        
        setTitle("Script Extension File");
        setDescription("Define name of Python script file and class name. The file and class are used for all tests in the testIDEA project.\n" +
                "These settings will be used to fill project properties found in File | Properties | Scripts.\n");
  
        // setControl(newControl);
        
        String [] moduleAndClass = extensionModuleAndClass.split("\\.", 2);
        if (moduleAndClass.length == 1) {
            m_extensionClassName = moduleAndClass[0].trim();
        } else if (moduleAndClass.length == 2) {
            m_extensionModuleName = (moduleAndClass[0] + ".py").trim();
            m_extensionClassName = moduleAndClass[1].trim();
        }
    }

    @Override
    public void createControl(Composite parent) {
      Composite container = new Composite(parent, SWT.NULL);

      container.setLayout(new MigLayout("fillx", "[min!][fill][min!]"));

      KGUIBuilder builder = new KGUIBuilder(container);

      builder.label("&Script file:");
      m_scriptFileNameBrowser = builder.createFileNameInput("Browse", 
                                                            "", 
                                                            "wrap", 
                                                            "Browse for Python extension script", 
                                                            new String[]{"*.py", "*.*"}, 
                                                            true,
                                                            SWT.SAVE);
      m_scriptFileNameBrowser.setToolTipText( 
                     "Name of the script file. There can be only one script file per testIDEA project.");
      builder.label("By default (if no path is specified) the script file is created in winIDEA workspace folder.", 
              "skip, span 2, wrap");

      m_overwriteGrp = builder.group("File exists - specify open mode", "skip, gapbottom 20, wrap");
      
      m_isOverwriteRb = m_overwriteGrp.radio("&Overwrite existing file", "wrap");
      UiTools.setToolTip(m_isOverwriteRb, "If selected, then the generated script will overwrite the existing file.\n" +
              "Class definition will be generated.\n" +
              "This button is enabled only when the script file already exists.\n");

      m_isAppendRb = m_overwriteGrp.radio("&Append script functions to existing file", "wrap");
      UiTools.setToolTip(m_isAppendRb, "If selected, then the generated methods will be appended to the existing file.\n" +
              "Class definition will NOT be generated - it is assumed it already exists.\n" +
              "This button is enabled only when the script file already exists.\n");

      

      builder.label("&Class name:");
      m_classNameTxt = builder.text("wrap", SWT.BORDER);
      UiTools.setToolTip(m_classNameTxt, "Class name without module name.");

      m_isOpenScriptInEditorCb = builder.checkBox("O&pen the generated script in editor after this wizard closes",
              "span 2, gaptop 25, wrap");

      builder.label("Sc&ript editor:", "gapleft 25");
      m_editorPath = builder.createFileNameInput("Browse", "", "wrap", "Select editor executable", 
                                                 new String[]{"*.exe", "*.bat", "*.*"}, 
                                                 false,
                                                 SWT.OPEN);
      
      // listeners
      KeyListener textKeyListener = new KeyListener() {

          @Override
          public void keyPressed(KeyEvent e) {}

          @Override
          public void keyReleased(KeyEvent e) {
              setPageComplete(isPageValid());
          }
      };
      
      m_classNameTxt.addKeyListener(textKeyListener);
      m_scriptFileNameBrowser.getInputField().addKeyListener(textKeyListener);
      m_scriptFileNameBrowser.getInputField().addModifyListener(new ModifyListener() {

          @Override
          public void modifyText(ModifyEvent e) {
              enableControls();
          }
      });


      m_isAppendRb.addSelectionListener(new SelectionListener() {

          @Override
          public void widgetSelected(SelectionEvent e) {
              setPageComplete(isPageValid());
              enableControls();              
          }

          @Override
          public void widgetDefaultSelected(SelectionEvent e) {}
      });
      
      initFields();
      
      setControl(container);
      // !m_extensionClassName.isEmpty()  &&  !m_extensionModuleName.isEmpty()
      setPageComplete(isPageValid());
    }

   
    private boolean isPageValid() {
    
        boolean isFileNameDefined = !m_scriptFileNameBrowser.getText().trim().isEmpty();
        if (!isFileNameDefined) {
            setErrorMessage("Please define script file name!");
            return false;
        }
        
        Path script = Paths.get(m_scriptFileNameBrowser.getText());
        if (Files.exists(script)   &&  Files.isDirectory(script)) {
            setErrorMessage("Please define script file name - currently it contains directory name!");
            return false;
        }
        
        boolean isFileExists = isScriptExists();
        
        boolean isClassNameDefined = !m_classNameTxt.getText().trim().isEmpty();  
        boolean isAppendMode = m_isAppendRb.getSelection(); 
                
        boolean isPageValid = (isFileExists  &&  isAppendMode)  ||  isClassNameDefined;
        
        if (!isPageValid) {
            setErrorMessage("Class name is missing!");
        } else {
            setErrorMessage(null);
        }
        
        return isPageValid;
    }
    
    
    private void initFields() {

        String dir = Paths.get(ISysPathFileUtils.getIYAMLDir(), 
                               m_extensionModuleName).toString();

        m_classNameTxt.setText(m_extensionClassName);
        m_scriptFileNameBrowser.setText(dir);
        
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        m_isOpenScriptInEditorCb.setSelection(prefs.getBoolean(PREFS_IS_OPEN_EDITOR));
        if (prefs.getDefaultString(PREFS_PY_EDITOR_PATH).isEmpty()) {
            prefs.setDefault(PREFS_PY_EDITOR_PATH, "C:\\Python27\\Lib\\idlelib\\idle.bat");
        }
        m_editorPath.setText(prefs.getString(PREFS_PY_EDITOR_PATH));
        
        m_isAppendRb.setSelection(true);
        
        enableControls();
    }


    private void enableControls() {
        boolean scriptFileExists = isScriptExists();
        // m_isOverwriteRb.setEnabled(scriptFileExists);
        // m_isAppendRb.setEnabled(scriptFileExists);
        m_overwriteGrp.getParent().setVisible(scriptFileExists);
        
        // if file already exists, only methods will be appended, so class should not
        // be generated
        boolean isCreateClass = !scriptFileExists  ||  m_isOverwriteRb.getSelection();
        m_classNameTxt.setEnabled(isCreateClass);
        m_classNameTxt.setText(m_extensionClassName);
    }

    
    private boolean isScriptExists() {
        Path script = Paths.get(m_scriptFileNameBrowser.getText());
        
        return Files.exists(script)   &&  !Files.isDirectory(script)
                &&  Files.isRegularFile(script);
    }
    
    
    public void saveToPreferences() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        
        prefs.setValue(PREFS_IS_OPEN_EDITOR, m_isOpenScriptInEditorCb.getSelection());
        prefs.setValue(PREFS_PY_EDITOR_PATH, m_editorPath.getText().trim());
    }
     
    
    public boolean isOverwriteFile() {
        return m_isOverwriteRb.getSelection();
    }

    public String getScriptFileName() {
        return m_scriptFileNameBrowser.getText().trim();
    }

    public String getClassName() {
        return m_classNameTxt.getText().trim();
    }
    
    
    public boolean isOpenScriptInEditor() {
        return m_isOpenScriptInEditorCb.getSelection();
    }
    
    public String getPythonEditorPath() {
        return m_editorPath.getText().trim();
    }
}
