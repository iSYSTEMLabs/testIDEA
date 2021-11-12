/**
 * This package contains classes which manage collections of commonly used
 * content assist proposals. Mainly these are target identifiers, but may also
 * include plug-in specific items such as core IDs.<p>
 * 
 * The main class in this package is {@link GlobalsContainer}, which manages collections
 * of proposals. After instantiation call its {@link GlobalsContainer#refresh()} 
 * method and then proposals are available.<p>
 * 
 * <b>Usage</b><p>
 * 
 * Client should create a class, which implements interface IGlobalsConfiguration.
 * This class provides information required to create customized collections of
 * proposals.<p>
 * 
 * <b>Example of class extending <code>IGlobalsConfiguration:</code></b><p>
 * 
 * <i>Note:</i> This class is implemented as a singleton, and it also 
 * aggregates <code>GlobalsContainer</code>.<p>
 * 
 * <pre>
 * public class GlobalsConfiguration implements IGlobalsConfiguration {
 * 
 * private GlobalsContainer m_globalsContainer;
 * 
 *     private final static GlobalsConfiguration INSTANCE = new GlobalsConfiguration(); 
 *
 *     // Creates container and adds custom proposals provider to it.
 *     public GlobalsConfiguration() {
 *         
 *         m_globalsContainer = new GlobalsContainer(this, ConnectionProvider.instance());
 *         
 *         TestIdsGlobalsProvider testIDsGP = new TestIdsGlobalsProvider();
 *         m_globalsContainer.addProvider(GlobalsContainer.GC_TEST_IDS, testIDsGP);
 *         
 *         testIDsGP.refreshGlobals();
 *     }
 *
 *
 *     //@Override
 *     public boolean isUseQualifiedFuncNames() {
 *         TestSpecificationModel model = TestSpecificationModel.getActiveModel();
 *         return model.isUseQualifiedFuncNames();
 *     }
 *
 *    
 *     //@Override
 *     public String [] getCoreIds() {
 *         return TestSpecificationModel.getActiveModel().getCoreIDs();
 *     }
 *
 *
 *     //@Override
 *     public String[][] getCustomVars() {
 *         PersistVarsProposalsProvider persistPropsProvider = new PersistVarsProposalsProvider();
 *         return persistPropsProvider.getAllPersistVars();
 *     }
 *    
 *    
 *     public static GlobalsConfiguration instance() {
 *         return INSTANCE;
 *     }
 *    
 *    
 *     public GlobalsContainer getGlobalContainer() {
 *         return m_globalsContainer;
 *     }
 *
 *    
 *     public void refresh() {
 *         m_globalsContainer.refresh();
 *     }
 * }
 * </pre>
 * 
 * Another class intended to be instantiated by clients is <i>GlobalsSelectionControl</i>.
 * It implements combo box with proposals, and some optional <i>Refresh</i> and 
 * </i>Show Source buttons. <p>
 * Example:
 *
 * <pre>
 *         m_funcText = new GlobalsSelectionControl(builder.getParent(), 
 *                                                  "w 0:90%:100%, wrap",
 *                                                  null,
 *                                                  null,
 *                                                  SWT.NONE,
 *                                                  GlobalsContainer.GC_FUNCTIONS,
 *                                                  "",
 *                                                  true,
 *                                                  true,
 *                                                  ContentProposalAdapter.PROPOSAL_REPLACE,
 *                                                  UIPrefsPage.isShowContentProposalsOnExplicitCtrlSpace(),
 *                                                  GlobalsConfiguration.instance().getGlobalContainer(),
 *                                                  ConnectionProvider.instance());
 * <pre> 
 */
package si.isystem.commons.globals;
