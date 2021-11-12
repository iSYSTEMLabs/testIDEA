package si.isystem.itest.run;

public interface IDebugHandler {
    
    enum UserResponse {CLEANUP_AND_CONTINUE, SKIP_CLEANUP_AND_STOP};
    
    /**
     * This method should open a dialog, informing the user that test execution 
     * was terminated by an error, but target state is not modified, so he can 
     * see target state in wiINDEA. There should be a button to continue with 
     * cleanup or skip cleanup, when done.
     * 
     * @param ex exception that was thrown.
     * @return enum value indicating whether cleanup should be done or not
     */
    UserResponse handleException(Exception ex);
    
    /**
     * Returns last user's response when one of other method was executed. 
     * @return
     */
    UserResponse getLastResponse();

    /**
     * This method informs the user, that test execution has finished, but stack
     * contents is still there, so he can analyze it. Cleanup is performed 
     * regardless of return value.
     * 
     * @return for future compatibility, this method should always return null.
     */
    UserResponse waitForCleanup();
    
    /**
     * This method is called, when execution stops on breakpoint. User is given 
     * option to continue with the current test or terminate tests.
     */
    UserResponse handleUnexpectedStop();
}
