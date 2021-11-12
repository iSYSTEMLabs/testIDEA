package si.isystem.itest.common;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import si.isystem.connect.CTestSpecification.ETestScope;
import si.isystem.exceptions.SExceptionDialog;
import si.isystem.exceptions.SIllegalArgumentException;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.main.Activator;

/**
 * This class maintains a collection of all possible images for test tree, and other
 * images used in the application.
 * This reduces usage of image instances significantly, even is all possible 
 * combinations are needed. Images are not disposed, since they are needed until
 * the application ends.
 *  
 * @author markok
 *
 */
public class IconProvider {
	
	public final static String COMMONS_PLUGIN_ICONS_PATH = "platform:/plugin/si.isystem.commons.plugin/resources/icons";

    public final static IconProvider INSTANCE = new IconProvider();

    public enum EDerivedTestResultStatus {NO_RESULTS,   // none of the derived tests was executed 
        AT_LEAST_ONE_DERIVED_HAS_RESULT, // and all existing results are OK
        AT_LEAST_ONE_DERIVED_FAILED}

    private Image m_runnableUnitTestImg;
    private Image m_nonRunnableUnitTestImg;
    private Image m_nonRunnableSystemTestImg;
    private Image m_runnableSystemTestImg;

    private Image m_testGroupImg;

    private ImageDescriptor m_testErrOverlayDescriptor;
    private ImageDescriptor m_testOkOverlayDescriptor;
    private ImageDescriptor m_derivedTestErrOverlayDescriptor;
    private ImageDescriptor m_derivedTestOkOverlayDescriptor;

    private static Image m_editableNoInfoImg;
    private static Image m_nonEditableInfoImg;
    private static Image m_nonEditableNoInfoImg;
    private static Image m_availableInfoImg;

    private static Image m_testOkImg_16x16;
    private static Image m_testOkImg_7x8;
    private static Image m_testErrorImg_16x16;
    private static Image m_testErrorImg_7x8;
    private static Image m_testOkWInfoImg;
    private static Image m_testErrorWInfoImg;

    private static Image m_scriptStatusInfoImg;
    
    public enum EOverlayId {INFO_AVAILABLE,
                            EDITABLE_INFO, 
                            EDITABLE_NO_INFO, 
                            NONEDITABLE_INFO, 
                            NONEDITABLE_NO_INFO,
                            TEST_OK_OVERLAY, TEST_ERR_OVERLAY, 
                            TEST_OK__WINFO_OVERLAY, TEST_ERR_WINFO_OVERLAY, TEST_OK, TEST_ERR,
                            SCRIPT_STATUS_INFO};
    
    public enum EIconId {ERefresh, EHelpContents_10x10, EHelpContents_12x12, EHelpContents_16x16, 
                         EAddItem, EAddItemDisabled, 
                         EUndo, ERedo,
                         EConnectedToWinIDEA, EDisconnectedFromwinIDEA,
                         EStdVersion, EEvalVersion, EProVersion,
                         EAddTableColumn, EDeleteTableColumn, 
                         EUpInTable, EDownInTable,
                         EEmptyOverlay, // this overlay is used when table/cells are not editable
                         ECloseTab,
                         EDownArrow,
                         EListItems,
                         EWizard,
                         ETestGroup, 
                         ETestSpecNotInGroup,
                         ECore,
                         EPartition,
                         EModule,
                         EFunction, EDefaultText, ELinkToEditor, 
                         ESelectDeselectAll, EToggleInherit, 
                         EExtrapolate, EInterpolate, EExternalTools,                         
                         };               
                         
    /*
     *         icons          |   overlays               
     * Empty Defined Inactive | Merged OK Err 
     * -------------------------------------
     *   1                    |
     *   1                    |    1
     *   1                    |    1   1
     *   1                    |    1        1
     *           1            |    
     *           1            |    1
     *           1            |    1   1                         
     *           1            |    1        1                         
     *           1            |        1                         
     *           1            |             1
     *                   1    |    
     *                   1    |    1                         
     */
    public enum EEditorTreeIconId {EEmpty,
                                   EEmptyMerged, 
                                   EEmptyMergedOk, 
                                   EEmptyMergedErr,
                                   
                                   EDefined,
                                   EDefinedMerged,
                                   EDefinedOk,
                                   EDefinedErr,
                                   EDefinedMergedOk,
                                   EDefinedMergedErr,
                                   
                                   EInactive,
                                   EInactiveMerged
                                   };               
                             
                            
    class IconKey {
        boolean m_isRunnable;
        Boolean m_isTestError;
        EDerivedTestResultStatus m_derivedTestResultStatus;
        private ETestScope m_testType;


        /**
         * 
         * @param isRunnable
         * @param testScope if null group image is created, otherwise 
         *                  appropriate test case image is created.
         * @param isTestError
         * @param derivedTestResultStatus
         */
        public IconKey(boolean isRunnable,
                       ETestScope testScope,
                       Boolean isTestError,
                       EDerivedTestResultStatus derivedTestResultStatus) {
            m_isRunnable = isRunnable;
            m_testType = testScope;
            m_isTestError = isTestError;
            m_derivedTestResultStatus = derivedTestResultStatus;
        }

        
        Image createImage() {
            Image mainIconImg = null;
            
            if (m_testType == null) {
                mainIconImg = m_testGroupImg;
            } else {
                switch (m_testType) {
                case E_UNIT_TEST:
                    mainIconImg = m_runnableUnitTestImg;
                    if (!m_isRunnable) {
                        mainIconImg = m_nonRunnableUnitTestImg;
                    }
                    break;
                case E_SYSTEM_TEST:
                    mainIconImg = m_runnableSystemTestImg;
                    if (!m_isRunnable) {
                        mainIconImg = m_nonRunnableSystemTestImg;
                    }
                    break;
                default:
                    throw new SIllegalStateException("Unknown test type when creating icons!");
                }
            }
            
            ImageDescriptor currentTestResultStatusIconTopLeft = null;
            if (m_isTestError != null) {
                if (m_isTestError) {
                    currentTestResultStatusIconTopLeft = m_testErrOverlayDescriptor;
                } else {
                    currentTestResultStatusIconTopLeft = m_testOkOverlayDescriptor;
                }
            }
            
            ImageDescriptor derivedTestsResultStatusIconTopRight = null;
            switch (m_derivedTestResultStatus) {
            case AT_LEAST_ONE_DERIVED_FAILED:
                derivedTestsResultStatusIconTopRight = m_derivedTestErrOverlayDescriptor;
                break;
            case AT_LEAST_ONE_DERIVED_HAS_RESULT:
                derivedTestsResultStatusIconTopRight = m_derivedTestOkOverlayDescriptor;
                break;
            case NO_RESULTS:
            default:
                break;  // already null
            }
            
            DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(mainIconImg,
                                                    new ImageDescriptor[]{currentTestResultStatusIconTopLeft,
                                                                          derivedTestsResultStatusIconTopRight});
            return overlayIcon.createImage();
        }
        
        
        int toInt() {
            int testType = 0;

            if (m_testType == null) {
                testType = ETestScope.values().length;
            } else {
                testType = m_testType.ordinal();
            }

            return (m_isRunnable ? 1000 : 0) +
                    (m_isTestError == null ? 300 : m_isTestError.booleanValue() ? 400 : 500) +
                    m_derivedTestResultStatus.ordinal() * 10 + testType;
        }
    }
    
    
    private Map<Integer, Image> m_groupImageMap = new TreeMap<>();
    private Map<Integer, Image> m_testSpecImageMap = new TreeMap<>();
    private Map<EIconId, Image> m_generalIconMap = new TreeMap<>();
    private Map<EEditorTreeIconId, Image> m_editorTreeIconMap = new TreeMap<>();
    
    private IconProvider() {
        try {
            m_testErrOverlayDescriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/test_err_overlay.gif");
            m_testOkOverlayDescriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/test_ok_overlay.gif");

            m_derivedTestErrOverlayDescriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/derived_test_err_overlay.gif");
            
            m_derivedTestOkOverlayDescriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/derived_test_ok_overlay.gif");

            ImageDescriptor descriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/runnable_unit_test_16.gif");

            if (descriptor != null) {
                m_runnableUnitTestImg = descriptor.createImage();
            }

            descriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/not_runnable_unit_test_16.gif");

            if (descriptor != null) {
                m_nonRunnableUnitTestImg = descriptor.createImage(); 
            }
            
            descriptor = 
                    AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                    "icons/runnable_system_test_16.gif");

            if (descriptor != null) {
                m_runnableSystemTestImg = descriptor.createImage(); 
            }
            descriptor = 
                    AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                                                               "icons/not_runnable_system_test_16.gif");

            if (descriptor != null) {
                m_nonRunnableSystemTestImg = descriptor.createImage(); 
            }
        } catch (Exception ex) {
            SExceptionDialog.open(Activator.getShell(), "Error loading icons!", ex);
        }
        
        createIconsForDerivedResults(false, ETestScope.E_UNIT_TEST, null);
        createIconsForDerivedResults(false, ETestScope.E_UNIT_TEST, Boolean.FALSE);
        createIconsForDerivedResults(false, ETestScope.E_UNIT_TEST, Boolean.TRUE);
        
        createIconsForDerivedResults(true, ETestScope.E_UNIT_TEST, null);
        createIconsForDerivedResults(true, ETestScope.E_UNIT_TEST, Boolean.FALSE);
        createIconsForDerivedResults(true, ETestScope.E_UNIT_TEST, Boolean.TRUE);
        
        createIconsForDerivedResults(false, ETestScope.E_SYSTEM_TEST, null);
        createIconsForDerivedResults(false, ETestScope.E_SYSTEM_TEST, Boolean.FALSE);
        createIconsForDerivedResults(false, ETestScope.E_SYSTEM_TEST, Boolean.TRUE);
        
        createIconsForDerivedResults(true, ETestScope.E_SYSTEM_TEST, null);
        createIconsForDerivedResults(true, ETestScope.E_SYSTEM_TEST, Boolean.FALSE);
        createIconsForDerivedResults(true, ETestScope.E_SYSTEM_TEST, Boolean.TRUE);
        
        createIconsForTestGroups();
        
        m_generalIconMap.put(EIconId.ERefresh, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/enabled/refresh.gif"));
        m_generalIconMap.put(EIconId.EHelpContents_10x10, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/help_contents10x10.gif"));
        m_generalIconMap.put(EIconId.EHelpContents_12x12, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/help_contents12x12.gif"));
        m_generalIconMap.put(EIconId.EHelpContents_16x16, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/help_contents.gif"));
        m_generalIconMap.put(EIconId.EAddItem, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/enabled/add_exc.gif"));
        m_generalIconMap.put(EIconId.EAddItemDisabled, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/disabled/add_exc_gray.gif"));
        m_generalIconMap.put(EIconId.EAddTableColumn, createImage(null, "icons/add_table_column.gif"));
        m_generalIconMap.put(EIconId.EDeleteTableColumn, createImage(null, "icons/remove_table_column.gif"));
        m_generalIconMap.put(EIconId.EUpInTable, createImage(null, "icons/up_in_table_ovr.gif"));
        m_generalIconMap.put(EIconId.EDownInTable, createImage(null, "icons/down_in_table_ovr.gif"));
        m_generalIconMap.put(EIconId.EUndo, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/enabled/undo_edit.gif"));
        m_generalIconMap.put(EIconId.ERedo, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/enabled/redo_edit.gif"));
        m_generalIconMap.put(EIconId.EEmptyOverlay, createImage(null, "icons/empty_overlay.gif"));
        m_generalIconMap.put(EIconId.ECloseTab, createImage(null, "icons/close_editor_tab.gif"));        
        m_generalIconMap.put(EIconId.EConnectedToWinIDEA, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/connected.png"));
        m_generalIconMap.put(EIconId.EDisconnectedFromwinIDEA, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/disconnected.png"));
        m_generalIconMap.put(EIconId.EDownArrow, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/down_arrow.png"));
        m_generalIconMap.put(EIconId.EListItems, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/list_items.gif"));
        m_generalIconMap.put(EIconId.ETestSpecNotInGroup, createImage(null, "icons/inactive_section_16.gif"));
                             
        m_generalIconMap.put(EIconId.EStdVersion, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/stdVersion.png"));
        m_generalIconMap.put(EIconId.EEvalVersion, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/evalVersion.png"));
        m_generalIconMap.put(EIconId.EProVersion, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/proVersion.png"));
        
        m_generalIconMap.put(EIconId.EWizard, createImage(null, "icons/wizard.png"));
        m_generalIconMap.put(EIconId.ECore, createImage(null, "icons/core.png"));
        m_generalIconMap.put(EIconId.EPartition, createImage(null, "icons/downloadFile.gif"));
        m_generalIconMap.put(EIconId.EModule, createImage(null, "icons/disc.gif"));
        m_generalIconMap.put(EIconId.EFunction, createImage(null, "icons/function.png"));
        m_generalIconMap.put(EIconId.ELinkToEditor, createImage(null, COMMONS_PLUGIN_ICONS_PATH + "/enabled/linkToEditor.png"));
        
        m_generalIconMap.put(EIconId.EDefaultText, createImage(null, "icons/defaultText.png"));

        m_generalIconMap.put(EIconId.ESelectDeselectAll, createImage(null, "icons/selected_mode.png"));
        m_generalIconMap.put(EIconId.EToggleInherit, createImage(null, "icons/merge_toggle.png"));
        m_generalIconMap.put(EIconId.EExtrapolate, createImage(null, "icons/extrapolate.png"));
        m_generalIconMap.put(EIconId.EInterpolate, createImage(null, "icons/interpolate.png"));
        m_generalIconMap.put(EIconId.EExternalTools, createImage(null, "icons/external_tools.png"));

        createEditorTreeIcons();
    }


    private void createIconsForDerivedResults(boolean isRunnable,
                                              ETestScope testType,
                                              Boolean isTestError) {
        IconKey iconKey;
        iconKey = new IconKey(isRunnable, testType, isTestError, EDerivedTestResultStatus.NO_RESULTS);
        m_testSpecImageMap.put(iconKey.toInt(), iconKey.createImage());
        iconKey = new IconKey(isRunnable, testType, isTestError, EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_HAS_RESULT);
        m_testSpecImageMap.put(iconKey.toInt(), iconKey.createImage());
        iconKey = new IconKey(isRunnable, testType, isTestError, EDerivedTestResultStatus.AT_LEAST_ONE_DERIVED_FAILED);
        m_testSpecImageMap.put(iconKey.toInt(), iconKey.createImage());
    }

    
    private void createIconsForTestGroups() {
        
        m_testGroupImg = createImage(null, "icons/test_group.gif");
        
        for (Boolean isTestError : new Boolean[]{null, Boolean.FALSE, Boolean.TRUE}) {
            for (EDerivedTestResultStatus childResultStatus : EDerivedTestResultStatus.values()) {
                IconKey iconKey = new IconKey(true, null, isTestError, childResultStatus);
                m_groupImageMap.put(iconKey.toInt(), iconKey.createImage());
            }
        }
        
//        DecorationOverlayIcon overlayIcon = 
//                      new DecorationOverlayIcon(m_testGroupImg,
//                                                new ImageDescriptor[]{m_testErrOverlayDescriptor});
//
//        m_testGroupErrorImg = overlayIcon.createImage();
//        
//        overlayIcon = new DecorationOverlayIcon(m_testGroupImg,
//                                                new ImageDescriptor[]{m_testOkOverlayDescriptor});
//        m_testGroupOkImg = overlayIcon.createImage();
    }
    
    
    private void createEditorTreeIcons() {

        ImageDescriptor mergedOverlayDesc = getDescriptor("merged_overlay.gif");
        
        Image emptyImg = getDescriptor("not_runnable_unit_test_16.gif").createImage(); 
        m_editorTreeIconMap.put(EEditorTreeIconId.EEmpty, emptyImg);
        
        Image overlayIcon = createOverlayImg(emptyImg, null, mergedOverlayDesc);
        m_editorTreeIconMap.put(EEditorTreeIconId.EEmptyMerged, overlayIcon);
        
        overlayIcon = createOverlayImg(emptyImg, m_testOkOverlayDescriptor, mergedOverlayDesc);
        m_editorTreeIconMap.put(EEditorTreeIconId.EEmptyMergedOk, overlayIcon);
        
        overlayIcon = createOverlayImg(emptyImg, m_testErrOverlayDescriptor, mergedOverlayDesc);
        m_editorTreeIconMap.put(EEditorTreeIconId.EEmptyMergedErr, overlayIcon);

        
        Image definedImg = getDescriptor("runnable_unit_test_16.gif").createImage();
        m_editorTreeIconMap.put(EEditorTreeIconId.EDefined, definedImg);

        overlayIcon = createOverlayImg(definedImg, m_testOkOverlayDescriptor);
        m_editorTreeIconMap.put(EEditorTreeIconId.EDefinedOk, overlayIcon);
        
        overlayIcon = createOverlayImg(definedImg, m_testErrOverlayDescriptor);
        m_editorTreeIconMap.put(EEditorTreeIconId.EDefinedErr, overlayIcon);
        
        overlayIcon = createOverlayImg(definedImg, null, mergedOverlayDesc);
        m_editorTreeIconMap.put(EEditorTreeIconId.EDefinedMerged, overlayIcon);

        overlayIcon = createOverlayImg(definedImg, m_testOkOverlayDescriptor, mergedOverlayDesc);
        m_editorTreeIconMap.put(EEditorTreeIconId.EDefinedMergedOk, overlayIcon);
        
        overlayIcon = createOverlayImg(definedImg, m_testErrOverlayDescriptor, mergedOverlayDesc);
        m_editorTreeIconMap.put(EEditorTreeIconId.EDefinedMergedErr, overlayIcon);
        
        
        Image inactiveImg = getDescriptor("inactive_section_16.gif").createImage();
        m_editorTreeIconMap.put(EEditorTreeIconId.EInactive, inactiveImg);

        overlayIcon = createOverlayImg(inactiveImg, null, mergedOverlayDesc);
        m_editorTreeIconMap.put(EEditorTreeIconId.EInactiveMerged, overlayIcon);
}


    private ImageDescriptor getDescriptor(String imageFileName) 
    {
        ImageDescriptor descriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                "icons/" + imageFileName);

        if (descriptor == null) {
            throw new SIllegalStateException("Icon 'icons/" + imageFileName + "' not found!");
        }
        
        return descriptor;
    }
    
    
    private Image createOverlayImg(Image img, ImageDescriptor ... over) {
        
        DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(img, over);
        return overlayIcon.createImage();
    }

    
//    public Image getTestGroupIcon(boolean isResult, boolean isError) {
//    
//        if (!isResult) {
//            return m_testGroupImg;
//        }
//        
//        if (isError) {
//            return m_testGroupErrorImg;
//        }
//            
//        return m_testGroupOkImg;
//    }

    
    public Image getTreeViewIcon(boolean isRunnable,
                                 ETestScope testScope,
                                 Boolean isTestError,
                                 EDerivedTestResultStatus derivedTestResultStatus) {

        IconKey iconKey = new IconKey(isRunnable, testScope, isTestError, derivedTestResultStatus);
        Image img = m_testSpecImageMap.get(iconKey.toInt()); 
        return img;
    }
    
        
    public Image getGroupIcon(Boolean isTestError,
                              EDerivedTestResultStatus derivedTestResultStatus) {

        IconKey iconKey = new IconKey(true, null, isTestError, derivedTestResultStatus);
        Image img = m_groupImageMap.get(iconKey.toInt()); 
        return img;
    }
    
        
    public Image getEditorTreeIcon(EEditorTreeIconId iconId) {
        return m_editorTreeIconMap.get(iconId);
    }
    
    
    public Image getIcon(EIconId iconId) {
        return m_generalIconMap.get(iconId);
    }
    
    
    public static Image getOverlay(EOverlayId overlayId) {
        switch (overlayId) {
        case INFO_AVAILABLE:
            m_availableInfoImg = createImage(m_availableInfoImg, "icons/info_available_ovr.gif");
            return m_availableInfoImg;
            
        case EDITABLE_INFO:
            return FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage();
        
        case EDITABLE_NO_INFO:
            m_editableNoInfoImg = createImage(m_editableNoInfoImg, "icons/info_na_active.gif");
            return m_editableNoInfoImg;

        case NONEDITABLE_INFO:
            m_nonEditableInfoImg = createImage(m_nonEditableInfoImg, "icons/info_gray_ovr.gif");
            return m_nonEditableInfoImg;
            
        case NONEDITABLE_NO_INFO:
            m_nonEditableNoInfoImg = createImage(m_nonEditableNoInfoImg, "icons/info_na_inactive.gif");
            return m_nonEditableNoInfoImg; 
            
        case TEST_OK_OVERLAY:
            m_testOkImg_7x8 = createImage(m_testOkImg_7x8, "icons/test_ok_overlay.gif");
            return m_testOkImg_7x8;
            
        case TEST_ERR_OVERLAY:
            m_testErrorImg_7x8 = createImage(m_testErrorImg_7x8, "icons/test_err_overlay.gif");
            return m_testErrorImg_7x8;

        // TEST_OK and TEST_ERR are the same as TEST_OK_OVERLAY and TEST_ERR_OVERLAY,
        // except the size is 16x16 instead of 7x8. They are used in the Expected table,
        // because if small icon is used in table first, then the bigger one is scaled
        // to the size of the small one by TableViewer (a bug?).
        case TEST_OK:
            m_testOkImg_16x16 = createImage(m_testOkImg_16x16, "icons/test_ok.gif");
            return m_testOkImg_16x16;
            
        case TEST_ERR:
            m_testErrorImg_16x16 = createImage(m_testErrorImg_16x16, "icons/test_err.gif");
            return m_testErrorImg_16x16;

        case TEST_OK__WINFO_OVERLAY:
            m_testOkWInfoImg = createImage(m_testOkWInfoImg, "icons/test_ok_winfo_overlay.gif");
            return m_testOkWInfoImg;
            
        case TEST_ERR_WINFO_OVERLAY:
            m_testErrorWInfoImg = createImage(m_testErrorWInfoImg, "icons/test_err_winfo_overlay.gif");
            return m_testErrorWInfoImg;
            
        case SCRIPT_STATUS_INFO:
            m_scriptStatusInfoImg = createImage(m_scriptStatusInfoImg, "icons/derived_test_ok_overlay.gif");
            return m_scriptStatusInfoImg;
        default:
        }
        throw new SIllegalArgumentException("Invalid overlay ID!").add("overlayId", overlayId);
    }
    
    
    private static Image createImage(Image ref, String imageFileName) {
        if (ref == null) {
            ImageDescriptor descriptor = 
                AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
                                                           imageFileName);

            if (descriptor != null) {
                ref = descriptor.createImage();
            }
        }
        return ref;
    }
}
