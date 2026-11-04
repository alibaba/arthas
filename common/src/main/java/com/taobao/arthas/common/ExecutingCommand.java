package com.taobao.arthas.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class for executing on the command line and returning the result of
 * execution.
 *
 * @author alessandro[at]perucchi[dot]org
 */
public class ExecutingCommand {

    private ExecutingCommand() {
    }

    /**
     * Executes a command on the native command line and returns the result.
     *
     * @param cmdToRun
     *            Command to run
     * @return A list of Strings representing the result of the command, or empty
     *         string if the command failed
     */
    public static List<String> runNative(String cmdToRun) {
        String[] cmd = cmdToRun.split(" ");
        return runNative(cmd);
    }

    /**
     * Executes a command on the native command line and returns the result line by
     * line.
     *
     * @param cmdToRunWithArgs
     *            Command to run and args, in an array
     * @return A list of Strings representing the result of the command, or empty
     *         string if the command failed
     */
    public static List<String> runNative(String[] cmdToRunWithArgs) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmdToRunWithArgs);
        } catch (SecurityException e) {
            AnsiLog.trace("Couldn't run command {}:", Arrays.toString(cmdToRunWithArgs));
            AnsiLog.trace(e);
            return new ArrayList<String>(0);
        } catch (IOException e) {
            AnsiLog.trace("Couldn't run command {}:", Arrays.toString(cmdToRunWithArgs));
            AnsiLog.trace(e);
            return new ArrayList<String>(0);
        }

        ArrayList<String> sa = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sa.add(line);
            }
            p.waitFor();
        } catch (IOException e) {
            AnsiLog.trace("Problem reading output from {}:", Arrays.toString(cmdToRunWithArgs));
            AnsiLog.trace(e);
            return new ArrayList<String>(0);
        } catch (InterruptedException ie) {
            AnsiLog.trace("Problem reading output from {}:", Arrays.toString(cmdToRunWithArgs));
            AnsiLog.trace(ie);
            Thread.currentThread().interrupt();
        } finally {
            IOUtils.close(reader);
        }
        return sa;
    }

    /**
     * Return first line of response for selected command.
     *
     * @param cmd2launch
     *            String command to be launched
     * @return String or empty string if command failed
     */
    public static String getFirstAnswer(String cmd2launch) {
        return getAnswerAt(cmd2launch, 0);
    }

    /**
     * Return response on selected line index (0-based) after running selected
     * command.
     *
     * @param cmd2launch
     *            String command to be launched
     * @param answerIdx
     *            int index of line in response of the command
     * @return String whole line in response or empty string if invalid index or
     *         running of command fails
     */
    public static String getAnswerAt(String cmd2launch, int answerIdx) {
        List<String> sa = ExecutingCommand.runNative(cmd2launch);

        if (answerIdx >= 0 && answerIdx < sa.size()) {
            return sa.get(answerIdx);
        }
        return "";
    }

}
