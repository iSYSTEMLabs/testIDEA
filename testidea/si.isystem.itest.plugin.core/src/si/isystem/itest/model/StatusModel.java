package si.isystem.itest.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import si.isystem.connect.CTestGroup;
import si.isystem.connect.CTestGroupResult;
import si.isystem.connect.CTestReportContainer;
import si.isystem.connect.CTestResult;
import si.isystem.connect.CTestTreeNode;
import si.isystem.exceptions.SIllegalStateException;
import si.isystem.itest.common.DataUtils;
import si.isystem.itest.model.StatusModelEvent.EEventType;
import si.isystem.itest.model.StatusTableLine.StatusType;
import si.isystem.itest.model.status.IStatusModelListener;

/**
 * This class contains data for Status view. It stores output produced during 
 * test run and results of test case model verification, when performed.
 * Errors and warnings during editing are written directly to StatusView's 
 * detailed pane. 
 * 
 * @author markok
 *
 */
public class StatusModel {

    // model data
    private List<StatusTableLine> m_tableModel = new ArrayList<StatusTableLine>();
    private List<StatusTableLine> m_fileResults = new ArrayList<StatusTableLine>();
    private List<TestSpecStatus> m_testSpecStatus;
    private StringBuilder m_detailPaneText = new StringBuilder();
    // private StatusType m_detailPaneStatus;
    
    // listeners
    private List<IStatusModelListener> m_listeners = new ArrayList<>();
    private StatusType m_highestStatus = StatusType.OK;
    private int m_numLinesBeforeAppend;
    
    private static StatusModel INSTANCE = new StatusModel();

    class SuccessCounters {
        int okCount = 0;
        int errorCount = 0;
        int fatalCount = 0;
        
        boolean isResult() {
            return okCount + errorCount + fatalCount > 0;
        }
        
        boolean isOK() {
            return errorCount == 0  &&  fatalCount == 0;
        }
        
        boolean isError() {
            return errorCount > 0;
        }
        
        boolean isFatal() {
            return fatalCount > 0;
        }
    }
    
    
    private StatusModel() 
    {}
    
    
    public static StatusModel instance() {
        return INSTANCE;
    }
    
    
    public void addListener(IStatusModelListener listner) {
        m_listeners.add(listner);
    }
    

    public void removeListener(IStatusModelListener listner) {
        m_listeners.remove(listner);
    }
    

    /**
     * Clears all stored results.
     */
    public void clear() {
        m_tableModel.clear();
        m_fileResults.clear();
        if (m_testSpecStatus != null) {
            m_testSpecStatus.clear();
        }
        
        m_numLinesBeforeAppend = 0;
        m_detailPaneText = new StringBuilder();
        
        fireEvent(new StatusModelEvent(EEventType.ERefresh, null));
    }
    
    
    public void setTestSpecStatus(List<TestSpecStatus> testSpecStatus) {
        m_testSpecStatus = testSpecStatus;
    }
    

    public void setDetailPaneText(StatusTableLine.StatusType status, String text) {
        m_numLinesBeforeAppend = 0;
        m_detailPaneText = new StringBuilder(text);
        
        fireEvent(new StatusModelEvent(EEventType.ESetDetailPane, text, status));
    }

    
    /**
     * Appends info to current detailed pane. This information will be deleted 
     * on next update. This method should be used to set warnings/errors for
     * currently selected test case.
     * @param status
     * @param text
     */
    public void appendDetailPaneText(StatusType status, String text) {
        // when appending, user usually wants to see start of the last message
        int numLines = DataUtils.countLines(m_detailPaneText);
        
        m_detailPaneText.append(text);
        
        fireEvent(new StatusModelEvent(EEventType.ESetDetailPane, 
                                       m_detailPaneText.toString(),
                                       status, numLines));
    }
    
    
    /**
     * Call this method from non-UI threads. Afer finish, UI thread should call 
     * showTextFromNonUIThread().
     */
    public void appendDetailPaneTextFromNonUIThread(StatusType status, String text) {
        m_numLinesBeforeAppend = DataUtils.countLines(m_detailPaneText);
        m_detailPaneText.append(text);
        
        if (status.ordinal() > m_highestStatus.ordinal()) {
            m_highestStatus = status;
        }
    }
    

    /**
     * @see #appendDetailPaneTextFromNonUIThread  
     */
    public void showTextFromNonUIThread() {
        fireEvent(new StatusModelEvent(EEventType.ESetDetailPane, 
                                       m_detailPaneText.toString(),
                                       m_highestStatus, 
                                       m_numLinesBeforeAppend));
        m_highestStatus = StatusType.OK;
    }
    
    /**
     * This method adds one line, which contains results summary for one file.
     *  
     * @param fileName
     * @param resultsMap
     */
    public void appendFileResults(String fileName, 
                                  CTestReportContainer resultsMap) {
        
        if (resultsMap == null) {
            return;
        }

        SuccessCounters testCounters = new SuccessCounters();
        SuccessCounters groupCounters = new SuccessCounters();
        
        countSuccess(resultsMap, testCounters, groupCounters, null, null);

        String testedEntity = "'" + new Path(fileName).lastSegment() + "'";

        createResultStatisticsLine(fileName, m_fileResults, 
                                   testCounters, groupCounters, 
                                   testedEntity, null);
        
        fireEvent(new StatusModelEvent(EEventType.ERefresh, null));
    }
    
    
    /**
     * This method adds one status line for file. It should be used to notify 
     * user about problems/warnings related to iyaml file found during testing, 
     * for example exceptions while saving reports or report config without 
     * report file name assigned.
     *  
     * @param modelFileName
     * @param status
     * @param message
     */
    public void appendFileMessage(String modelFileName,
                                  StatusType status,
                                  String message) {
        
        StatusTableLine statusLine = new StatusTableLine(status, 
                                                         modelFileName,
                                                         "---", 
                                                         modelFileName,
                                                         message,
                                                         null);
        
        m_fileResults.add(statusLine);
        
        fireEvent(new StatusModelEvent(EEventType.ERefresh, statusLine));
    }
    
    
    /**
     * Updates status view when another iyaml file (model) is selected.
     * @param model may be null 
     */
    public void updateTestResults(TestSpecificationModel model) {
        
        if (model != null) {
            m_tableModel = getContentResults(model.getModelFileName(), model);
        } else {
            m_tableModel = getContentResults("", null);
        }

        StatusTableLine statusLine = null;
        
        if (m_tableModel.size() > 0) {
            statusLine = m_tableModel.get(0); // the selected line
        }
        
        fireEvent(new StatusModelEvent(EEventType.ERefresh, statusLine));
    }
    
    
    /**
     * This method appends line to table, but does not add it to model - it should
     * be used only during test run, to inform the user about current errors so that
     * he can terminate the test. After the test completes, table is refreshed
     * from this model.
     * 
     * @param result
     */
    public void appendResult(CTestResult result) {
        
        StatusTableLine statusLine = appendResultToModel("<runningTest>", result, null);
        
        if (statusLine != null) {
            fireEvent(new StatusModelEvent(EEventType.EAppend, statusLine));
        }
    }
    
    
    private List<StatusTableLine> getContentResults(String fileName, 
                                                    TestSpecificationModel model) {
        
        List<StatusTableLine> list = new ArrayList<StatusTableLine>();

        if (model != null) {
            SuccessCounters testCounters = new SuccessCounters();
            SuccessCounters groupCounters = new SuccessCounters();
            
            CTestReportContainer testReport = model.getTestReportContainer();
            countSuccess(testReport,
                         testCounters,
                         groupCounters,
                         fileName,
                         list);
            
            String testedEntity = "selected editor";

            createResultStatisticsLine(fileName,
                                       list,
                                       testCounters,
                                       groupCounters,
                                       testedEntity,
                                       m_detailPaneText);
        }

        // add warnings from the test case model
        if (m_testSpecStatus != null) {
         
            for (TestSpecStatus status : m_testSpecStatus) {
                StatusType severity;
                switch (status.getSeverity()) {
                case IStatus.OK:
                    severity = StatusType.OK;
                    break;
                case IStatus.INFO:
                    severity = StatusType.INFO;
                    break;
                case IStatus.WARNING:
                    severity = StatusType.WARNING;
                    break;
                case IStatus.ERROR:
                    severity = StatusType.ERROR;
                    break;
                default:
                    severity = StatusType.ERROR;
                }

                CTestTreeNode testTreeNode = status.getTestTreeNode();
                String uiLbl = testTreeNode.getUILabel();
                String idAndName[] = uiLbl.split(":", 2);
                if (idAndName.length < 2) { // should never happen
                    idAndName = new String[]{"?", "?"};
                }

                list.add(new StatusTableLine(severity, 
                                             fileName,
                                             idAndName[0],
                                             idAndName[1],
                                             testTreeNode,
                                             status.getMessage()));
            }
        }
        return list;
    }


    private void countSuccess(CTestReportContainer testReport,
                              SuccessCounters testCounters,
                              SuccessCounters groupCounters,
                              String fileName,
                              List<StatusTableLine> list) {
        
        testReport.resetTestResultIterator(); 
        while (testReport.hasNextTestResult()) {
            CTestResult result = testReport.nextTestResult();

            if (fileName != null) {
                appendResultToModel(fileName, result, list);
            }

            if (result.isException()) {
                testCounters.fatalCount++;
            } else if (result.isError()) {
                testCounters.errorCount++;
            } else {
                testCounters.okCount++;
            }
        }

        testReport.resetGroupResultIterator(); 
        while (testReport.hasNextGroupResult()) {
            CTestGroupResult result = testReport.nextGroupResult();

            if (fileName != null) {
                appendResultToModel(fileName, result, list);
            }

            if (result.isException()) {
                groupCounters.fatalCount++;
            } else if (result.isError()) {
                groupCounters.errorCount++;
            } else {
                groupCounters.okCount++;
            }
        }
    }


    private void createResultStatisticsLine(String fileName,
                                            List<StatusTableLine> list,
                                            SuccessCounters testCounters,
                                            SuccessCounters groupCounters,
                                            String testedEntity,
                                            StringBuilder detailPaneText) {
        
        if (testCounters.isResult()  &&  testCounters.isOK()  && groupCounters.isOK()) {
            list.add(0, new StatusTableLine(StatusType.OK, 
                                            fileName,
                                            "---", 
                                            fileName,
                                            "All tests for " + testedEntity + " completed successfully!\n" +
                                                    "Number of tests: " + testCounters.okCount + "\n\n" +
                                                    (detailPaneText != null ? detailPaneText : ""),
                                                    null));
        } else if ((testCounters.isError()  ||  groupCounters.isError())  && 
                   (!testCounters.isFatal()  &&  !groupCounters.isFatal())) {
            list.add(0, new StatusTableLine(StatusType.ERROR, 
                                            fileName,
                                            "---", 
                                            fileName,
                                            composeStatusMessage(testedEntity,
                                                                 testCounters,
                                                                 groupCounters,
                                                                 detailPaneText),
                                                                 null));
        } else if (testCounters.isFatal()  ||  groupCounters.isFatal()) {
            list.add(0, new StatusTableLine(StatusType.FATAL, 
                                            fileName,
                                            "---", 
                                            fileName,
                                            composeStatusMessage(testedEntity,
                                                                 testCounters,
                                                                 groupCounters,
                                                                 detailPaneText),
                                                                 null));
        }
    }
    
    
    private StatusTableLine appendResultToModel(String fileName, 
                                                CTestResult result, 
                                                List<StatusTableLine> model) {
        
        StatusTableLine statusLine = null;
        if (result.isException()) {
            statusLine = new StatusTableLine(StatusType.FATAL, 
                                             fileName,
                                             result.getTestId(), 
                                             result.getFunction(), 
                                             result.getExceptionString(),
                                             result);
        } else if (result.isError()) {
            statusLine = new StatusTableLine(StatusType.ERROR, 
                                             fileName,
                                             result.getTestId(), 
                                             result.getFunction(), 
                                             result.toUIString(),
                                             result);
        } 
        
        if (statusLine != null  &&  model != null) {
            model.add(statusLine);
        }
        
        return statusLine;
    }
    
    
    private StatusTableLine appendResultToModel(String fileName, 
                                                CTestGroupResult result, 
                                                List<StatusTableLine> model) {
        
        StatusTableLine statusLine = null;
        CTestGroup group = CTestGroup.cast(result.getParent());
        String id = "";
        String label = "";
        if (group != null) {
            id = group.getId();
            label = group.getUILabel();
        }
        
        if (result.isException()) {
            statusLine = new StatusTableLine(StatusType.FATAL, 
                                             fileName,
                                             id, 
                                             label, 
                                             result,
                                             result.getExceptionString());
        } else if (result.isError()) {
            statusLine = new StatusTableLine(StatusType.ERROR, 
                                             fileName,
                                             id, 
                                             label,
                                             result,
                                             result.toUIString());
        } 
        
        if (statusLine != null  &&  model != null) {
            model.add(statusLine);
        }
        
        return statusLine;
    }
    
    
    private String composeStatusMessage(String fileNameOrEditor, 
                                        SuccessCounters testCounters,
                                        SuccessCounters groupCounters,
                                        StringBuilder stdOutErr) {
        
        StringBuilder sb = new StringBuilder("Test report for " + fileNameOrEditor + ", ");
        
        int allTests = testCounters.okCount + testCounters.errorCount + testCounters.fatalCount;
        int allGroups = groupCounters.okCount + groupCounters.errorCount + groupCounters.fatalCount;
        sb.append(allTests).append(" test(s), ");
        sb.append(allGroups).append(" group(s):\n");
        
        
        if (testCounters.okCount == 1) {
            sb.append("- 1 test (")
              .append(getPercentage(testCounters.okCount, allTests))
              .append("%) completed successfully\n");
        } else if (testCounters.okCount > 1) {
            sb.append("- ").append(testCounters.okCount).append(" tests (")
              .append(getPercentage(testCounters.okCount, allTests)).append("%) completed successfully\n");
        }
        
        if (testCounters.errorCount == 1) {
            sb.append("- 1 test (").append(getPercentage(testCounters.errorCount, allTests))
              .append("%) failed (invalid results)\n");
        } else if (testCounters.errorCount > 1) {
            sb.append("- ").append(testCounters.errorCount).append(" tests (")
              .append(getPercentage(testCounters.errorCount, allTests)).append("%) failed (invalid results)\n");
        }

        if (testCounters.fatalCount == 1) {
            sb.append("- 1 test (").append(getPercentage(testCounters.fatalCount, allTests))
              .append("%) with error (failed execution)\n");
        } else if (testCounters.fatalCount > 1) {
            sb.append("- ").append(testCounters.fatalCount).append(" tests (")
              .append(getPercentage(testCounters.fatalCount, allTests)).append("%) with error (failed execution)\n");
        }
        
        if (groupCounters.okCount == 1) {
            sb.append("- 1 group (")
              .append(getPercentage(groupCounters.okCount, allGroups))
              .append("%) passed\n");
        } else if (groupCounters.okCount > 1) {
            sb.append("- ").append(groupCounters.okCount).append(" groups (")
              .append(getPercentage(groupCounters.okCount, allGroups)).append("%) completed successfully\n");
        }
        
        if (groupCounters.errorCount == 1) {
            sb.append("- 1 group (").append(getPercentage(groupCounters.errorCount, allGroups))
              .append("%) failed (invalid results)\n");
        } else if (groupCounters.errorCount > 1) {
            sb.append("- ").append(groupCounters.errorCount).append(" groups (")
              .append(getPercentage(groupCounters.errorCount, allGroups)).append("%) failed (invalid results)\n");
        }

        if (groupCounters.fatalCount == 1) {
            sb.append("- 1 group (").append(getPercentage(groupCounters.fatalCount, allGroups))
              .append("%) with error (failed evaluation)\n");
        } else if (groupCounters.fatalCount > 1) {
            sb.append("- ").append(groupCounters.fatalCount).append(" groups (")
              .append(getPercentage(groupCounters.fatalCount, allGroups)).append("%) with error (failed execution)\n");
        }
        
        if (stdOutErr != null) {
            sb.append("\n\n").append(stdOutErr);
        }
        
        return sb.toString();
    }
    

    private String getPercentage(int value, int sum) {
        return Integer.toString(Math.round(value * 100f / sum));
    }
    
    
    private void fireEvent(StatusModelEvent event) {
        
        for (IStatusModelListener listener : m_listeners) {
            switch (event.getEventType()) {
            case EAppend:
                listener.appendLine(event);
                break;
            case ERefresh:
                listener.refresh(event);
                break;
            case ESetDetailPane:
                listener.setDetailPaneText(event);
                break;
            default:
                throw new SIllegalStateException("Unknown status model event type:!").
                                    add("eventType", event.getEventType());
            }
        }
    }


    public StatusTableLine[] getElements() {
        List <StatusTableLine> summary = new ArrayList<>(m_fileResults.size() + 
                                                         m_tableModel.size());
        summary.addAll(m_fileResults);
        summary.addAll(m_tableModel);
        return summary.toArray(new StatusTableLine[0]);    
    }
}
