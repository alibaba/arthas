package com.taobao.arthas.core.command.monitor200;

/**
 * @author ralf0131 2017-01-11 14:57.
 */
public class CompleteContext {

    private CompleteState state;

    public CompleteContext() {
        this.state = CompleteState.INIT;
    }

    public void setState(CompleteState state) {
        this.state = state;
    }

    public CompleteState getState() {
        return state;
    }

    /**
     * The state transition diagram is:
     * INIT -> CLASS_NAME -> METHOD_NAME -> FINISHED
     */
    enum CompleteState {

        /**
         * the state that nothing is completed
         */
        INIT,

        /**
         * the state that class name is completed
         */
        CLASS_COMPLETED,

        /**
         * the state that method name is completed
         */
        METHOD_COMPLETED,

        /**
         * the state that express is completed
         */
        EXPRESS_COMPLETED,

        /**
         * the state that condition-express is completed
         */
        CONDITION_EXPRESS_COMPLETED
    }
}
