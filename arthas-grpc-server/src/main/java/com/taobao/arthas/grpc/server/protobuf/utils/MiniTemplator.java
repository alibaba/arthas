// Copyright 2003-2010 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms
// of any of the following licenses:
//
//  EPL, Eclipse Public License, http://www.eclipse.org/legal
//  LGPL, GNU Lesser General Public License, http://www.gnu.org/licenses/lgpl.html
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.

package com.taobao.arthas.grpc.server.protobuf.utils;;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A compact template engine for HTML files.
 *
 * <p>
 * Template syntax:<br>
 * <pre>
 *    Variables:
 *       ${VariableName}
 *
 *    Blocks:
 *       &lt;!-- $beginBlock blockName --&gt;
 *         ... block contents ...
 *       &lt;!-- $endBlock blockName --&gt;
 *
 *    Conditional blocks:
 *       &lt;!-- $if flag1 flag2 --&gt;
 *         ... included if flag1 or flag2 is set ...
 *       &lt;!-- $elseIf !flag3 flag4 --&gt;
 *         ... included if flag3 is not set or flag4 is set ...
 *       &lt;!-- $else --&gt;
 *         ... included if none of the above conditions is met ...
 *       &lt;!-- $endIf --&gt;
 *
 *    Short form of conditional blocks:
 *    (only recognized if {@link TemplateSpecification#shortFormEnabled TemplateSpecification.shortFormEnabled} is <code>true</code>)
 *       &lt;$? flag1 flag2 &gt;
 *         ... included if flag1 or flag2 is set ...
 *       &lt;$: !flag3 flag4 &gt;
 *         ... included if flag3 is not set or flag4 is set ...
 *       &lt;$:&gt;
 *         ... included if none of the above conditions is met ...
 *       &lt;$/?&gt;
 *    Example:
 *       &lt;$?de&gt; Hallo Welt!
 *       &lt;$:fr&gt; Bonjour tout le monde!
 *       &lt;$:  &gt; Hello world!
 *       &lt;$/?&gt;
 *
 *    Include a subtemplate:
 *       &lt;!-- $include relativeFileName --&gt;</pre>
 *
 * <p>
 * General remarks:</p>
 * <ul>
 *  <li>Variable names, block names, condition flags and commands (e.g. "$beginBlock") are case-insensitive.</li>
 *  <li>The same variable may be used multiple times within a template.</li>
 *  <li>Multiple blocks with the same name may occur within a template.</li>
 *  <li>Blocks can be nested.</li>
 *  <li>Conditional blocks ($if) and includes ($include) are resolved when the template is parsed.
 *      Parsing is done within the MiniTemplator constructor.
 *      Condition flags can be passed to the constructor using {@link TemplateSpecification}.
 *  <li>Normal blocks ($beginBlock) must be added (and can be repeated) by the application program using <code>addBlock()</code>.
 *  <li>The {@link MiniTemplatorCache} class may be used to cache MiniTemplator objects with parsed templates.</li>
 *  </ul>
 *
 * <p>
 * Project home page: <a href="http://www.source-code.biz/MiniTemplator">www.source-code.biz/MiniTemplator</a><br>
 * Author: Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
 */
public class MiniTemplator {

//--- exceptions -----------------------------------------------------

    /**
     * Thrown when a syntax error is encountered within the template.
     */
    public static class TemplateSyntaxException extends RuntimeException {
        private static final long serialVersionUID = 1;
        public TemplateSyntaxException (String msg) {
            super("Syntax error in template: " + msg); }}

    /**
     * Thrown when {@link MiniTemplator#setVariable(String, String, boolean) Minitemplator.setVariable}
     * is called with a <code>variableName</code> that is not defined
     * within the template and the <code>isOptional</code> parameter is <code>false</code>.
     */
    public static class VariableNotDefinedException extends RuntimeException {
        private static final long serialVersionUID = 1;
        public VariableNotDefinedException (String variableName) {
            super("Variable \"" + variableName + "\" not defined in template."); }}

    /**
     * Thrown when {@link MiniTemplator#addBlock Minitemplator.addBlock}
     * is called with a <code>blockName</code> that is not defined
     * within the template.
     */
    public static class BlockNotDefinedException extends RuntimeException {
        private static final long serialVersionUID = 1;
        public BlockNotDefinedException (String blockName) {
            super("Block \"" + blockName + "\" not defined in template."); }}

//--- public nested classes ------------------------------------------

    /**
     * Specifies the parameters for constructing a {@link MiniTemplator} object.
     */
    public static class TemplateSpecification {                // template specification

        /**
         * The file name of the template file.
         */
        public String             templateFileName;

        /**
         * The path of the base directory for reading subtemplate files.
         * This path is used to convert the relative paths of subtemplate files (specified with the $include commands)
         * into absolute paths.
         * If this field is null, the parent directory of the main template file (specified by <code>templateFileName</code>) is used.
         */
        public String             subtemplateBasePath;

        /**
         * The character set to be used for reading and writing files.
         * This charset is used for reading the template and subtemplate files and for
         * writing output with {@link #generateOutput(String outputFileName)}.
         * If this field is null, the default charset of the Java VM is used.
         */
        public Charset            charset;

        /**
         * The contents of the template file.
         * This field may be used instead of <code>templateFileName</code> to pass the template text in memory.
         * If this field is not null, <code>templateFileName</code> will be ignored.
         */
        public String             templateText;

        /**
         * Flags for the conditional commands ($if, $elseIf).
         * A set of flag names, that can be used with the $if and $elseIf commands.
         * The flag names are case-insensitive.
         */
        public Set<String>        conditionFlags;

        /**
         * Enables the short form syntax for conditional blocks.
         */
        public boolean            shortFormEnabled; }

//--- private nested classes -----------------------------------------

    private static class BlockDynTabRec {                      // block dynamic data table record structure
        int                       instances;                    // number of instances of this block
        int                       firstBlockInstNo;             // block instance no of first instance of this block or -1
        int                       lastBlockInstNo;              // block instance no of last instance of this block or -1
        int                       currBlockInstNo; }            // current block instance no, used during generation of output file
    private static class BlockInstTabRec {                     // block instance table record structure
        int                       blockNo;                      // block number
        int                       instanceLevel;                // instance level of this block
        // InstanceLevel is an instance counter per block.
        // (In contrast to blockInstNo, which is an instance counter over the instances of all blocks)
        int                       parentInstLevel;              // instance level of parent block
        int                       nextBlockInstNo;              // pointer to next instance of this block or -1
        // Forward chain for instances of same block.
        String[]                  blockVarTab; }                // block instance variables

//--- private variables ----------------------------------------------

    private MiniTemplatorParser  mtp;                          // contains the parsed template
    private Charset              charset;                      // charset used for reading and writing files
    private String               subtemplateBasePath;          // base path for relative file names of subtemplates, may be null

    private String[]             varValuesTab;                 // variable values table, entries may be null

    private BlockDynTabRec[]     blockDynTab;                  // dynamic block-specific values
    private BlockInstTabRec[]    blockInstTab;                 // block instances table
    // This table contains an entry for each block instance that has been added.
    // Indexed by BlockInstNo.
    private int                  blockInstTabCnt;              // no of entries used in BlockInstTab

//--- constructors ---------------------------------------------------

    /**
     * Constructs a MiniTemplator object.
     * <p>During construction, the template and subtemplate files are read and parsed.
     * <p>Note: The {@link MiniTemplatorCache} class may be used to cache MiniTemplator objects.
     * @param  templateSpec             the template specification.
     * @throws TemplateSyntaxException  when a syntax error is detected within the template.
     * @throws IOException              when an i/o error occurs while reading the template.
     */
    public MiniTemplator (TemplateSpecification templateSpec)
            throws IOException, TemplateSyntaxException {
        init(templateSpec); }

    /**
     * Constructs a MiniTemplator object by specifying only the file name.
     * <p>This is a convenience constructor that may be used when only the file name has to be specified.
     * @param  templateFileName         the file name of the template file.
     * @throws TemplateSyntaxException  when a syntax error is detected within the template.
     * @throws IOException              when an i/o error occurs while reading the template.
     * @see #MiniTemplator(TemplateSpecification)
     */
    public MiniTemplator (String templateFileName)
            throws IOException, TemplateSyntaxException {
        TemplateSpecification templateSpec = new TemplateSpecification();
        templateSpec.templateFileName = templateFileName;
        init(templateSpec); }

    private void init (TemplateSpecification templateSpec)
            throws IOException, TemplateSyntaxException {
        charset = templateSpec.charset;
        if (charset == null) {
            charset = Charset.defaultCharset(); }
        subtemplateBasePath = templateSpec.subtemplateBasePath;
        if (subtemplateBasePath == null && templateSpec.templateFileName != null) {
            subtemplateBasePath = new File(templateSpec.templateFileName).getParent(); }
        String templateText = templateSpec.templateText;
        if (templateText == null && templateSpec.templateFileName != null) {
            templateText = readFileIntoString(templateSpec.templateFileName); }
        if (templateText == null) {
            throw new IllegalArgumentException("No templateFileName or templateText specified."); }
        mtp = new MiniTemplatorParser(templateText, templateSpec.conditionFlags, templateSpec.shortFormEnabled, this);
        reset(); }

    /**
     * Dummy constructor, used internally in newInstance().
     */
    protected MiniTemplator() {}

    /**
     * Allocates a new uninitialized MiniTemplator object.
     * This method is intended to be overridden in a derived class.
     * It is called from cloneReset() to create a new MiniTemplator object.
     */
    protected MiniTemplator newInstance() {
        return new MiniTemplator(); }

//--- loadSubtemplate ------------------------------------------------

    /**
     * Loads the template string of a subtemplate (used for the $Include command).
     * This method can be overridden in a subclass, to load subtemplates from
     * somewhere else, e.g. from a database.
     * <p>This implementation of the method interprets <code>subtemplateName</code>
     * as a relative file path name and reads the template string from that file.
     * {@link MiniTemplator.TemplateSpecification#subtemplateBasePath} is used to convert
     * the relative path of the subtemplate into an absolute path.
     * @param  subtemplateName     the name of the subtemplate.
     *        Normally a relative file path.
     *        This is the argument string that was specified with the "$Include" command.
     *        If the string has quotes, the quotes are removed before this method is called.
     * @return the template text string of the subtemplate.
     */
    protected String loadSubtemplate (String subtemplateName) throws IOException {
        String fileName = new File(subtemplateBasePath, subtemplateName).getPath();
        return readFileIntoString(fileName); }

//--- build up (template variables and blocks) ------------------------

    /**
     * Resets the MiniTemplator object to the initial state.
     * All variable values are cleared and all added block instances are deleted.
     * This method can be used to produce another HTML page with the same
     * template. It is faster than creating another MiniTemplator object,
     * because the template does not have to be read and parsed again.
     */
    public void reset() {
        if (varValuesTab == null) {
            varValuesTab = new String[mtp.varTabCnt]; }
        else {
            for (int varNo=0; varNo<mtp.varTabCnt; varNo++) {
                varValuesTab[varNo] = null; }}
        if (blockDynTab == null) {
            blockDynTab = new BlockDynTabRec[mtp.blockTabCnt]; }
        for (int blockNo=0; blockNo<mtp.blockTabCnt; blockNo++) {
            BlockDynTabRec bdtr = blockDynTab[blockNo];
            if (bdtr == null) {
                bdtr = new BlockDynTabRec();
                blockDynTab[blockNo] = bdtr; }
            bdtr.instances = 0;
            bdtr.firstBlockInstNo = -1;
            bdtr.lastBlockInstNo = -1; }
        blockInstTabCnt = 0; }

    /**
     * Clones this MiniTemplator object and resets the clone.
     * This method is used to copy a MiniTemplator object.
     * It is fast, because the template does not have to be parsed again,
     * and the internal data structures that contain the parsed template
     * information are shared among the clones.
     * <p>This method is used by the {@link MiniTemplatorCache} class to
     * clone the cached MiniTemplator objects.
     */
    public MiniTemplator cloneReset() {
        MiniTemplator m = newInstance();
        m.mtp = mtp;                                            // the MiniTemplatorParser object is shared among the clones
        m.charset = charset;
        // (subtemplateBasePath does not have to be copied, because the subtemplates have already been read)
        m.reset();
        return m; }

    /**
     * Sets a template variable.
     * <p>For variables that are used in blocks, the variable value
     * must be set before <code>addBlock()</code> is called.
     * @param  variableName   the name of the variable to be set. Case-insensitive.
     * @param  variableValue  the new value of the variable. May be <code>null</code>.
     * @param  isOptional     specifies whether an exception should be thrown when the
     *    variable does not exist in the template. If <code>isOptional</code> is
     *    <code>false</code> and the variable does not exist, an exception is thrown.
     * @throws VariableNotDefinedException when no variable with the
     *    specified name exists in the template and <code>isOptional</code> is <code>false</code>.
     */
    public void setVariable (String variableName, String variableValue, boolean isOptional)
            throws VariableNotDefinedException {
        int varNo = mtp.lookupVariableName(variableName);
        if (varNo == -1) {
            if (isOptional) {
                return; }
            throw new VariableNotDefinedException(variableName); }
        varValuesTab[varNo] = variableValue; }

    /**
     * Sets a template variable.
     * <p>Convenience method for: <code>setVariable (variableName, variableValue, false)</code>
     * @param  variableName    the name of the variable to be set. Case-insensitive.
     * @param  variableValue   the new value of the variable. May be <code>null</code>.
     * @throws VariableNotDefinedException when no variable with the
     *    specified name exists in the template.
     * @see #setVariable(String, String, boolean)
     */
    public void setVariable (String variableName, String variableValue)
            throws VariableNotDefinedException {
        setVariable(variableName, variableValue, false); }

    /**
     * Sets a template variable to an integer value.
     * <p>Convenience method for: <code>setVariable (variableName, Integer.toString(variableValue))</code>
     * @param  variableName    the name of the variable to be set. Case-insensitive.
     * @param  variableValue   the new value of the variable.
     * @throws VariableNotDefinedException when no variable with the
     *    specified name exists in the template.
     */
    public void setVariable (String variableName, int variableValue)
            throws VariableNotDefinedException {
        setVariable(variableName, Integer.toString(variableValue)); }

    /**
     * Sets an optional template variable.
     * <p>Convenience method for: <code>setVariable (variableName, variableValue, true)</code>
     * @param  variableName    the name of the variable to be set. Case-insensitive.
     * @param  variableValue   the new value of the variable. May be <code>null</code>.
     * @see #setVariable(String, String, boolean)
     */
    public void setVariableOpt (String variableName, String variableValue) {
        setVariable(variableName, variableValue, true); }

    /**
     * Sets an optional template variable to an integer value.
     * <p>Convenience method for: <code>setVariableOpt (variableName, Integer.toString(variableValue))</code>
     * @param  variableName    the name of the variable to be set. Case-insensitive.
     * @param  variableValue   the new value of the variable.
     */
    public void setVariableOpt (String variableName, int variableValue) {
        // We want to avoid the integer to string conversion if the template variable does not exist.
        int varNo = mtp.lookupVariableName(variableName);
        if (varNo == -1) {
            return; }
        varValuesTab[varNo] = Integer.toString(variableValue); }

    /**
     * Sets a template variable to an escaped value.
     * <p>Convenience method for: <code>setVariable (variableName, MiniTemplator.escapeHtml(variableValue), isOptional)</code>
     * @param  variableName   the name of the variable to be set.
     * @param  variableValue  the new value of the variable. May be <code>null</code>.
     *    Special HTML/XML characters are escaped.
     * @param  isOptional     specifies whether an exception should be thrown when the
     *    variable does not exist in the template. If <code>isOptional</code> is
     *    <code>false</code> and the variable does not exist, an exception is thrown.
     * @throws VariableNotDefinedException when no variable with the
     *    specified name exists in the template and <code>isOptional</code> is <code>false</code>.
     * @see #setVariable(String, String, boolean)
     * @see #escapeHtml(String)
     */
    public void setVariableEsc (String variableName, String variableValue, boolean isOptional)
            throws VariableNotDefinedException {
        setVariable(variableName, escapeHtml(variableValue), isOptional); }

    /**
     * Sets a template variable to an escaped value.
     * <p>Convenience method for: <code>setVariable (variableName, MiniTemplator.escapeHtml(variableValue), false)</code>
     * @param  variableName   the name of the variable to be set. Case-insensitive.
     * @param  variableValue  the new value of the variable. May be <code>null</code>.
     *    Special HTML/XML characters are escaped.
     * @throws VariableNotDefinedException when no variable with the
     *    specified name exists in the template.
     * @see #setVariable(String, String, boolean)
     * @see #escapeHtml(String)
     */
    public void setVariableEsc (String variableName, String variableValue)
            throws VariableNotDefinedException {
        setVariable(variableName, escapeHtml(variableValue), false); }

    /**
     * Sets an optional template variable to an escaped value.
     * <p>Convenience method for: <code>setVariable (variableName, MiniTemplator.escapeHtml(variableValue), true)</code>
     * @param  variableName   the name of the variable to be set. Case-insensitive.
     * @param  variableValue  the new value of the variable. May be <code>null</code>.
     *    Special HTML/XML characters are escaped.
     * @see #setVariable(String, String, boolean)
     * @see #escapeHtml(String)
     */
    public void setVariableOptEsc (String variableName, String variableValue) {
        setVariable(variableName, escapeHtml(variableValue), true); }

    /**
     * Checks whether a variable with the specified name exists within the template.
     * @param  variableName  the name of the variable. Case-insensitive.
     * @return <code>true</code> if the variable exists.<br>
     *    <code>false</code> if no variable with the specified name exists in the template.
     */
    public boolean variableExists (String variableName) {
        return mtp.lookupVariableName(variableName) != -1; }

    /**
     * Returns a map with the names and current values of the template variables.
     */
    public Map<String, String> getVariables() {
        HashMap<String, String> map = new HashMap<String, String>(mtp.varTabCnt);
        for (int varNo = 0; varNo < mtp.varTabCnt; varNo++)
            map.put(mtp.varTab[varNo], varValuesTab[varNo]);
        return map; }

    /**
     * Adds an instance of a template block.
     * <p>If the block contains variables, these variables must be set
     * before the block is added.
     * If the block contains subblocks (nested blocks), the subblocks
     * must be added before this block is added.
     * If multiple blocks exist with the specified name, an instance
     * is added for each block occurrence.
     * @param  blockName  the name of the block to be added. Case-insensitive.
     * @param  isOptional specifies whether an exception should be thrown when the
     *    block does not exist in the template. If <code>isOptional</code> is
     *    <code>false</code> and the block does not exist, an exception is thrown.
     * @throws BlockNotDefinedException when no block with the specified name
     *    exists in the template and <code>isOptional</code> is <code>false</code>.
     */
    public void addBlock (String blockName, boolean isOptional)
            throws BlockNotDefinedException {
        int blockNo = mtp.lookupBlockName(blockName);
        if(blockNo == -1) {
            if (isOptional) {
                return; }
            throw new BlockNotDefinedException(blockName); }
        while (blockNo != -1) {
            addBlockByNo(blockNo);
            blockNo = mtp.blockTab[blockNo].nextWithSameName; }}

    /**
     * Adds an instance of a template block.
     * <p>Convenience method for: <code>addBlock (blockName, false)</code>
     * @param  blockName  the name of the block to be added. Case-insensitive.
     * @throws BlockNotDefinedException when no block with the specified name
     *    exists in the template.
     * @see #addBlock(String, boolean)
     */
    public void addBlock (String blockName)
            throws BlockNotDefinedException {
        addBlock(blockName, false); }

    /**
     * Adds an instance of an optional template block.
     * <p>Convenience method for: <code>addBlock (blockName, true)</code>
     * @param  blockName  the name of the block to be added. Case-insensitive.
     * @see #addBlock(String, boolean)
     */
    public void addBlockOpt (String blockName) {
        addBlock(blockName, true); }

    private void addBlockByNo (int blockNo) {
        MiniTemplatorParser.BlockTabRec btr = mtp.blockTab[blockNo];
        BlockDynTabRec bdtr = blockDynTab[blockNo];
        int blockInstNo = registerBlockInstance();
        BlockInstTabRec bitr = blockInstTab[blockInstNo];
        if (bdtr.firstBlockInstNo == -1) {
            bdtr.firstBlockInstNo = blockInstNo; }
        if (bdtr.lastBlockInstNo != -1) {
            blockInstTab[bdtr.lastBlockInstNo].nextBlockInstNo = blockInstNo; } // set forward pointer of chain
        bdtr.lastBlockInstNo = blockInstNo;
        bitr.blockNo = blockNo;
        bitr.instanceLevel = bdtr.instances++;
        if (btr.parentBlockNo == -1) {
            bitr.parentInstLevel = -1; }
        else {
            bitr.parentInstLevel = blockDynTab[btr.parentBlockNo].instances; }
        bitr.nextBlockInstNo = -1;
        if (btr.blockVarCnt > 0) {
            bitr.blockVarTab = new String[btr.blockVarCnt]; }
        for (int blockVarNo=0; blockVarNo<btr.blockVarCnt; blockVarNo++) {  // copy instance variables for this block
            int varNo = btr.blockVarNoToVarNoMap[blockVarNo];
            bitr.blockVarTab[blockVarNo] = varValuesTab[varNo]; }}

    // Returns the block instance number.
    private int registerBlockInstance() {
        int blockInstNo = blockInstTabCnt++;
        if (blockInstTab == null) {
            blockInstTab = new BlockInstTabRec[64]; }
        if (blockInstTabCnt > blockInstTab.length) {
            blockInstTab = (BlockInstTabRec[])MiniTemplatorParser.resizeArray(blockInstTab, 2*blockInstTabCnt); }
        blockInstTab[blockInstNo] = new BlockInstTabRec();
        return blockInstNo; }

    /**
     * Checks whether a block with the specified name exists within the template.
     * @param  blockName  the name of the block.
     * @return <code>true</code> if the block exists.<br>
     *    <code>false</code> if no block with the specified name exists in the template.
     */
    public boolean blockExists (String blockName) {
        return mtp.lookupBlockName(blockName) != -1; }

//--- output generation ----------------------------------------------

    /**
     * Generates the HTML page and writes it into a file.
     * @param  outputFileName  name of the file to which the generated HTML page will be written.
     * @throws IOException when an i/o error occurs while writing to the file.
     */
    public void generateOutput (String outputFileName)
            throws IOException {
        FileOutputStream stream = null;
        OutputStreamWriter writer = null;
        try {
            stream = new FileOutputStream(outputFileName);
            writer = new OutputStreamWriter(stream, charset);
            generateOutput(writer); }
        finally {
            if (writer != null) {
                writer.close(); }
            if (stream != null) {
                stream.close(); }}}

    /**
     * Generates the HTML page and writes it to a character stream.
     * @param  outputWriter  a character stream (<code>writer</code>) to which
     *    the HTML page will be written.
     * @throws IOException when an i/o error occurs while writing to the stream.
     */
    public void generateOutput (Writer outputWriter)
            throws IOException {
        String s = generateOutput();
        outputWriter.write(s); }

    /**
     * Generates the HTML page and returns it as a string.
     * @return A string that contains the generated HTML page.
     */
    public String generateOutput() {
        if (blockDynTab[0].instances == 0) {
            addBlockByNo(0); }                         // add main block
        for (int blockNo=0; blockNo<mtp.blockTabCnt; blockNo++) {
            BlockDynTabRec bdtr = blockDynTab[blockNo];
            bdtr.currBlockInstNo = bdtr.firstBlockInstNo; }
        StringBuilder out = new StringBuilder();
        writeBlockInstances(out, 0, -1);
        return out.toString(); }

    // Writes all instances of a block that are contained within a specific
// parent block instance.
// Called recursively.
    private void writeBlockInstances (StringBuilder out, int blockNo, int parentInstLevel) {
        BlockDynTabRec bdtr = blockDynTab[blockNo];
        while (true) {
            int blockInstNo = bdtr.currBlockInstNo;
            if (blockInstNo == -1) {
                break; }
            BlockInstTabRec bitr = blockInstTab[blockInstNo];
            if (bitr.parentInstLevel < parentInstLevel) {
                throw new AssertionError(); }
            if (bitr.parentInstLevel > parentInstLevel) {
                break; }
            writeBlockInstance(out, blockInstNo);
            bdtr.currBlockInstNo = bitr.nextBlockInstNo; }}

    private void writeBlockInstance (StringBuilder out, int blockInstNo) {
        BlockInstTabRec bitr = blockInstTab[blockInstNo];
        int blockNo = bitr.blockNo;
        MiniTemplatorParser.BlockTabRec btr = mtp.blockTab[blockNo];
        int tPos = btr.tPosContentsBegin;
        int subBlockNo = blockNo + 1;
        int varRefNo = btr.firstVarRefNo;
        while (true) {
            int tPos2 = btr.tPosContentsEnd;
            int kind = 0;                              // assume end-of-block
            if (varRefNo != -1 && varRefNo < mtp.varRefTabCnt) { // check for variable reference
                MiniTemplatorParser.VarRefTabRec vrtr = mtp.varRefTab[varRefNo];
                if (vrtr.tPosBegin < tPos) {
                    varRefNo++;
                    continue; }
                if (vrtr.tPosBegin < tPos2) {
                    tPos2 = vrtr.tPosBegin;
                    kind = 1; }}
            if (subBlockNo < mtp.blockTabCnt) {        // check for subblock
                MiniTemplatorParser.BlockTabRec subBtr = mtp.blockTab[subBlockNo];
                if (subBtr.tPosBegin < tPos) {
                    subBlockNo++;
                    continue; }
                if (subBtr.tPosBegin < tPos2) {
                    tPos2 = subBtr.tPosBegin;
                    kind = 2; }}
            if (tPos2 > tPos) {
                out.append(mtp.templateText.substring(tPos, tPos2)); }
            switch (kind) {
                case 0:                                 // end of block
                    return;
                case 1: {                               // variable
                    MiniTemplatorParser.VarRefTabRec vrtr = mtp.varRefTab[varRefNo];
                    if (vrtr.blockNo != blockNo) {
                        throw new AssertionError(); }
                    String variableValue = bitr.blockVarTab[vrtr.blockVarNo];
                    if (variableValue != null) {
                        out.append(variableValue); }
                    tPos = vrtr.tPosEnd;
                    varRefNo++;
                    break; }
                case 2: {                               // sub block
                    MiniTemplatorParser.BlockTabRec subBtr = mtp.blockTab[subBlockNo];
                    if (subBtr.parentBlockNo != blockNo) {
                        throw new AssertionError(); }
                    writeBlockInstances(out, subBlockNo, bitr.instanceLevel);  // recursive call
                    tPos = subBtr.tPosEnd;
                    subBlockNo++;
                    break; }}}}

//--- general utility routines ---------------------------------------

    // Reads the contents of a file into a string variable.
    private String readFileIntoString (String fileName)
            throws IOException {
        FileInputStream stream = null;
        InputStreamReader reader = null;
        try {
            stream = new FileInputStream(fileName);
            reader = new InputStreamReader(stream, charset);
            return readStreamIntoString(reader); }
        finally {
            if (reader != null) {
                reader.close(); }
            if (stream != null) {
                stream.close(); }}}

    // Reads the contents of a stream into a string variable.
    private static String readStreamIntoString (Reader reader)
            throws IOException {
        StringBuilder s = new StringBuilder();
        char a[] = new char[0x10000];
        while (true) {
            int l = reader.read(a);
            if (l == -1) {
                break; }
            if (l <= 0) {
                throw new IOException(); }
            s.append(a, 0, l); }
        return s.toString(); }

    /**
     * Escapes special HTML characters.
     * Replaces the characters &lt;, &gt;, &amp;, ' and " by their corresponding
     * HTML/XML character entity codes.
     * @param  s  the input string.
     * @return the escaped output string.
     */
    public static String escapeHtml (String s) {
        // (The code of this method is a bit redundant in order to optimize speed)
        if (s == null) {
            return null; }
        int sLength = s.length();
        boolean found = false;
        int p;
        loop1:
        for (p=0; p<sLength; p++) {
            switch (s.charAt(p)) {
                case '<': case '>': case '&': case '\'': case '"': found = true; break loop1; }}
        if (!found) {
            return s; }
        StringBuilder sb = new StringBuilder(sLength+16);
        sb.append(s.substring(0, p));
        for (; p<sLength; p++) {
            char c = s.charAt(p);
            switch (c) {
                case '<':  sb.append ("&lt;"); break;
                case '>':  sb.append ("&gt;"); break;
                case '&':  sb.append ("&amp;"); break;
                case '\'': sb.append ("&#39;"); break;
                case '"':  sb.append ("&#34;"); break;
                default:   sb.append (c); }}
        return sb.toString(); }

} // End class MiniTemplator

//====================================================================================================================

// MiniTemplatorParser is an immutable object that contains the parsed template text.
class MiniTemplatorParser {

//--- constants ------------------------------------------------------

    private static final int     maxNestingLevel  = 20;        // maximum number of block nestings
    private static final int     maxCondLevels    = 20;        // maximum number of nested conditional commands ($if)
    private static final int     maxInclTemplateSize = 1000000; // maximum length of template string when including subtemplates
    private static final String  cmdStartStr      = "<!--";    // command start string
    private static final String  cmdEndStr        = "-->";     // command end string
    private static final String  cmdStartStrShort = "<$";      // short form command start string
    private static final String  cmdEndStrShort   = ">";       // short form command end string

//--- nested classes -------------------------------------------------

    public static class VarRefTabRec {                         // variable reference table record structure
        int                       varNo;                        // variable no
        int                       tPosBegin;                    // template position of begin of variable reference
        int                       tPosEnd;                      // template position of end of variable reference
        int                       blockNo;                      // block no of the (innermost) block that contains this variable reference
        int                       blockVarNo; }                 // block variable no. Index into BlockInstTab.BlockVarTab
    public static class BlockTabRec {                          // block table record structure
        String                    blockName;                    // block name
        int                       nextWithSameName;             // block no of next block with same name or -1 (blocks are backward linked related to their position within the template)
        int                       tPosBegin;                    // template position of begin of block
        int                       tPosContentsBegin;            // template pos of begin of block contents
        int                       tPosContentsEnd;              // template pos of end of block contents
        int                       tPosEnd;                      // template position of end of block
        int                       nestingLevel;                 // block nesting level
        int                       parentBlockNo;                // block no of parent block
        boolean                   definitionIsOpen;             // true while $BeginBlock processed but no $EndBlock
        int                       blockVarCnt;                  // number of variables in block
        int[]                     blockVarNoToVarNoMap;         // maps block variable numbers to variable numbers
        int                       firstVarRefNo;                // variable reference no of first variable of this block or -1
        boolean                   dummy; }                      // true if this is a dummy block that will never be included in the output

//--- variables ------------------------------------------------------

    public  String               templateText;                 // contents of the template file
    private HashSet<String>      conditionFlags;               // set of the condition flags, converted to uppercase
    private boolean              shortFormEnabled;             // true to enable the short form of commands ("<$...>")

    public  String[]             varTab;                       // variables table, contains variable names, array index is variable no
    public  int                  varTabCnt;                    // no of entries used in VarTab
    private HashMap<String,Integer> varNameToNoMap;            // maps variable names to variable numbers
    public  VarRefTabRec[]       varRefTab;                    // variable references table
    // Contains an entry for each variable reference in the template. Ordered by templatePos.
    public  int                  varRefTabCnt;                 // no of entries used in VarRefTab

    public  BlockTabRec[]        blockTab;                     // Blocks table, array index is block no
    // Contains an entry for each block in the template. Ordered by tPosBegin.
    public  int                  blockTabCnt;                  // no of entries used in BlockTab
    private HashMap<String,Integer> blockNameToNoMap;          // maps block names to block numbers

    // The following variables are only used temporarilly during parsing of the template.
    private int                  currentNestingLevel;          // current block nesting level during parsing
    private int[]                openBlocksTab;                // indexed by the block nesting level
    // During parsing, this table contains the block numbers of the open parent blocks (nested outer blocks).
    private int                  condLevel;                    // current nesting level of conditional commands ($if), -1 = main level
    private boolean[]            condEnabled;                  // enabled/disables state for the conditions of each level
    private boolean[]            condPassed;                   // true if an enabled condition clause has already been processed (separate for each level)
    private MiniTemplator        miniTemplator;                // the MiniTemplator who created this parser object
    // The reference to the MiniTemplator object is only used to call MiniTemplator.loadSubtemplate().
    private boolean              resumeCmdParsingFromStart;    // true = resume command parsing from the start position of the last command

//--- constructor ----------------------------------------------------

    // (The MiniTemplator object is only passed to the parser, because the
// parser needs to call MiniTemplator.loadSubtemplate() to load subtemplates.)
    public MiniTemplatorParser (String templateText, Set<String> conditionFlags, boolean shortFormEnabled, MiniTemplator miniTemplator)
            throws MiniTemplator.TemplateSyntaxException {
        this.templateText = templateText;
        this.conditionFlags = createConditionFlagsSet(conditionFlags);
        this.shortFormEnabled = shortFormEnabled;
        this.miniTemplator = miniTemplator;
        parseTemplate();
        this.miniTemplator = null; }

    private HashSet<String> createConditionFlagsSet (Set<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return null; }
        HashSet<String> flags2 = new HashSet<String>(flags.size());
        for (String flag : flags) {
            flags2.add (flag.toUpperCase()); }
        return flags2; }

//--- template parsing -----------------------------------------------

    private void parseTemplate()
            throws MiniTemplator.TemplateSyntaxException {
        initParsing();
        beginMainBlock();
        parseTemplateCommands();
        endMainBlock();
        checkBlockDefinitionsComplete();
        if (condLevel != -1) {
            throw new MiniTemplator.TemplateSyntaxException ("$if without matching $endIf."); }
        parseTemplateVariables();
        associateVariablesWithBlocks();
        terminateParsing(); }

    private void initParsing() {
        varTab = new String[64];
        varTabCnt = 0;
        varNameToNoMap = new HashMap<String,Integer>();
        varRefTab = new VarRefTabRec[64];
        varRefTabCnt = 0;
        blockTab = new BlockTabRec[16];
        blockTabCnt = 0;
        currentNestingLevel = 0;
        blockNameToNoMap = new HashMap<String,Integer>();
        openBlocksTab = new int[maxNestingLevel+1];
        condLevel = -1;
        condEnabled = new boolean[maxCondLevels];
        condPassed = new boolean[maxCondLevels]; }

    private void terminateParsing() {
        openBlocksTab = null; }

    // Registers the main block.
// The main block is an implicitly defined block that covers the whole template.
    private void beginMainBlock() {
        int blockNo = registerBlock(null);                      // =0
        BlockTabRec btr = blockTab[blockNo];
        btr.tPosBegin = 0;
        btr.tPosContentsBegin = 0;
        openBlocksTab[currentNestingLevel] = blockNo;
        currentNestingLevel++; }

    // Completes the main block registration.
    private void endMainBlock() {
        BlockTabRec btr = blockTab[0];
        btr.tPosContentsEnd = templateText.length();
        btr.tPosEnd = templateText.length();
        btr.definitionIsOpen = false;
        currentNestingLevel--; }

//--- Template commands --------------------------------------------------------

    // Parses commands within the template in the format "<!-- $command parameters -->".
// If shortFormEnabled is true, the short form commands in the format "<$...>" are also recognized.
    private void parseTemplateCommands()
            throws MiniTemplator.TemplateSyntaxException {
        int p = 0;                                              // p is the current position within templateText
        while (true) {
            int p0 = templateText.indexOf(cmdStartStr, p);       // p0 is the start of the current command
            boolean shortForm = false;
            if (shortFormEnabled && p0 != p) {
                if (p0 == -1) {
                    p0 = templateText.indexOf(cmdStartStrShort, p);
                    shortForm = true; }
                else {
                    int p2 = templateText.substring(p, p0).indexOf(cmdStartStrShort);
                    if (p2 != -1) {
                        p0 = p + p2;
                        shortForm = true; }}}
            if (p0 == -1) {                                      // no more commands
                break; }
            conditionalExclude(p, p0);                           // process text up to the start of the current command
            if (shortForm) {                                     // short form command
                p = templateText.indexOf(cmdEndStrShort, p0 + cmdStartStrShort.length());
                if (p == -1) {                                    // if no terminating ">" is found, we process it as normal text
                    p = p0 + cmdStartStrShort.length();
                    conditionalExclude(p0, p);
                    continue; }
                p += cmdEndStrShort.length();
                String cmdLine = templateText.substring(p0 + cmdStartStrShort.length(), p - cmdEndStrShort.length());
                if (!processShortFormTemplateCommand(cmdLine, p0, p)) {
                    // If a short form command is not recognized, we process the whole command structure are normal text.
                    conditionalExclude(p0, p); }}
            else {                                              // normal (long) form command
                p = templateText.indexOf(cmdEndStr, p0 + cmdStartStr.length());
                if (p == -1) {
                    throw new MiniTemplator.TemplateSyntaxException("Invalid HTML comment in template at offset " + p0 + "."); }
                p += cmdEndStr.length();
                String cmdLine = templateText.substring(p0 + cmdStartStr.length(), p - cmdEndStr.length());
                resumeCmdParsingFromStart = false;
                if (!processTemplateCommand(cmdLine, p0, p)) {
                    conditionalExclude(p0, p); }                   // process as normal temlate text
                if (resumeCmdParsingFromStart) {                  // (if a subtemplate has been included)
                    p = p0; }}}}

    // Returns false if the command should be treatet as normal template text.
    private boolean processTemplateCommand (String cmdLine, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        int p0 = skipBlanks(cmdLine, 0);
        if (p0 >= cmdLine.length()) {
            return false; }
        int p = skipNonBlanks(cmdLine, p0);
        String cmd = cmdLine.substring(p0, p);
        String parms = cmdLine.substring(p);
        /* select */
        if (cmd.equalsIgnoreCase("$beginBlock")) {
            processBeginBlockCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$endBlock")) {
            processEndBlockCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$include")) {
            processIncludeCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$if")) {
            processIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$elseIf")) {
            processElseIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$else")) {
            processElseCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$endIf")) {
            processEndIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else {
            if (cmd.startsWith("$") && !cmd.startsWith("${")) {
                throw new MiniTemplator.TemplateSyntaxException("Unknown command \"" + cmd + "\" in template at offset " + cmdTPosBegin + "."); }
            else {
                return false; }}
        return true; }

    // Returns false if the command is not recognized and should be treatet as normal temlate text.
    private boolean processShortFormTemplateCommand (String cmdLine, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        int p0 = skipBlanks(cmdLine, 0);
        if (p0 >= cmdLine.length()) {
            return false; }
        int p = p0;
        char cmd1 = cmdLine.charAt(p++);
        if (cmd1 == '/' && p < cmdLine.length() && !Character.isWhitespace(cmdLine.charAt(p))) {
            p++; }
        String cmd = cmdLine.substring(p0, p);
        String parms = cmdLine.substring(p).trim();
        /* select */
        if (cmd.equals("?")) {
            processIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equals(":")) {
            if (parms.length() > 0) {
                processElseIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
            else {
                processElseCmd(parms, cmdTPosBegin, cmdTPosEnd); }}
        else if (cmd.equals("/?")) {
            processEndIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else {
            return false; }
        return true; }

    // Processes the $beginBlock command.
    private void processBeginBlockCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        if (conditionalExclude(cmdTPosBegin, cmdTPosEnd)) {
            return; }
        int p0 = skipBlanks(parms, 0);
        if (p0 >= parms.length()) {
            throw new MiniTemplator.TemplateSyntaxException("Missing block name in $BeginBlock command in template at offset " + cmdTPosBegin + "."); }
        int p = skipNonBlanks(parms, p0);
        String blockName = parms.substring(p0, p);
        if (!isRestOfStringBlank(parms, p)) {
            throw new MiniTemplator.TemplateSyntaxException("Extra parameter in $BeginBlock command in template at offset " + cmdTPosBegin + "."); }
        int blockNo = registerBlock(blockName);
        BlockTabRec btr = blockTab[blockNo];
        btr.tPosBegin = cmdTPosBegin;
        btr.tPosContentsBegin = cmdTPosEnd;
        openBlocksTab[currentNestingLevel] = blockNo;
        currentNestingLevel++;
        if (currentNestingLevel > maxNestingLevel) {
            throw new MiniTemplator.TemplateSyntaxException("Block nesting overflow for block \"" + blockName + "\" in template at offset " + cmdTPosBegin + "."); }}

    // Processes the $endBlock command.
    private void processEndBlockCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        if (conditionalExclude(cmdTPosBegin, cmdTPosEnd)) {
            return; }
        int p0 = skipBlanks(parms, 0);
        if (p0 >= parms.length()) {
            throw new MiniTemplator.TemplateSyntaxException("Missing block name in $EndBlock command in template at offset " + cmdTPosBegin + "."); }
        int p = skipNonBlanks(parms, p0);
        String blockName = parms.substring(p0, p);
        if (!isRestOfStringBlank(parms, p)) {
            throw new MiniTemplator.TemplateSyntaxException("Extra parameter in $EndBlock command in template at offset " + cmdTPosBegin + "."); }
        int blockNo = lookupBlockName(blockName);
        if (blockNo == -1) {
            throw new MiniTemplator.TemplateSyntaxException("Undefined block name \"" + blockName + "\" in $EndBlock command in template at offset " + cmdTPosBegin + "."); }
        currentNestingLevel--;
        BlockTabRec btr = blockTab[blockNo];
        if (!btr.definitionIsOpen) {
            throw new MiniTemplator.TemplateSyntaxException("Multiple $EndBlock command for block \"" + blockName + "\" in template at offset " + cmdTPosBegin + "."); }
        if (btr.nestingLevel != currentNestingLevel) {
            throw new MiniTemplator.TemplateSyntaxException("Block nesting level mismatch at $EndBlock command for block \"" + blockName + "\" in template at offset " + cmdTPosBegin + "."); }
        btr.tPosContentsEnd = cmdTPosBegin;
        btr.tPosEnd = cmdTPosEnd;
        btr.definitionIsOpen = false; }

    // Returns the block number of the newly registered block.
    private int registerBlock (String blockName) {
        int blockNo = blockTabCnt++;
        if (blockTabCnt > blockTab.length) {
            blockTab = (BlockTabRec[])resizeArray(blockTab, 2*blockTabCnt); }
        BlockTabRec btr = new BlockTabRec();
        blockTab[blockNo] = btr;
        btr.blockName = blockName;
        if (blockName != null) {
            btr.nextWithSameName = lookupBlockName(blockName); }
        else {
            btr.nextWithSameName = -1; }
        btr.nestingLevel = currentNestingLevel;
        if (currentNestingLevel > 0) {
            btr.parentBlockNo = openBlocksTab[currentNestingLevel-1]; }
        else {
            btr.parentBlockNo = -1; }
        btr.definitionIsOpen = true;
        btr.blockVarCnt = 0;
        btr.firstVarRefNo = -1;
        btr.blockVarNoToVarNoMap = new int[32];
        btr.dummy = false;
        if (blockName != null) {
            blockNameToNoMap.put(blockName.toUpperCase(), new Integer(blockNo)); }
        return blockNo; }

    // Registers a dummy block to exclude a range within the template text.
    private void excludeTemplateRange (int tPosBegin, int tPosEnd) {
        if (blockTabCnt > 0) {
            // Check whether we can extend the previous block.
            BlockTabRec btr = blockTab[blockTabCnt-1];
            if (btr.dummy && btr.tPosEnd == tPosBegin) {
                btr.tPosContentsEnd = tPosEnd;
                btr.tPosEnd = tPosEnd;
                return; }}
        int blockNo = registerBlock(null);
        BlockTabRec btr = blockTab[blockNo];
        btr.tPosBegin = tPosBegin;
        btr.tPosContentsBegin = tPosBegin;
        btr.tPosContentsEnd = tPosEnd;
        btr.tPosEnd = tPosEnd;
        btr.definitionIsOpen = false;
        btr.dummy = true; }

    // Checks that all block definitions are closed.
    private void checkBlockDefinitionsComplete()
            throws MiniTemplator.TemplateSyntaxException {
        for (int blockNo=0; blockNo<blockTabCnt; blockNo++) {
            BlockTabRec btr = blockTab[blockNo];
            if (btr.definitionIsOpen) {
                throw new MiniTemplator.TemplateSyntaxException("Missing $EndBlock command in template for block \"" + btr.blockName + "\"."); }}
        if (currentNestingLevel != 0) {
            throw new MiniTemplator.TemplateSyntaxException("Block nesting level error at end of template."); }}

    // Processes the $include command.
    private void processIncludeCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        if (conditionalExclude(cmdTPosBegin, cmdTPosEnd)) {
            return; }
        int p0 = skipBlanks(parms, 0);
        if (p0 >= parms.length()) {
            throw new MiniTemplator.TemplateSyntaxException("Missing subtemplate name in $Include command in template at offset " + cmdTPosBegin + "."); }
        int p;
        if (parms.charAt(p0) == '"') {                          // subtemplate name is quoted
            p0++;
            p = parms.indexOf('"', p0);
            if (p == -1) {
                throw new MiniTemplator.TemplateSyntaxException("Missing closing quote for subtemplate name in $Include command in template at offset " + cmdTPosBegin + "."); }}
        else {
            p = skipNonBlanks(parms, p0); }
        String subtemplateName = parms.substring(p0, p);
        p++;
        if (!isRestOfStringBlank(parms, p)) {
            throw new MiniTemplator.TemplateSyntaxException("Extra parameter in $Include command in template at offset " + cmdTPosBegin + "."); }
        insertSubtemplate(subtemplateName, cmdTPosBegin, cmdTPosEnd); }

    private void insertSubtemplate (String subtemplateName, int tPos1, int tPos2) {
        if (templateText.length() > maxInclTemplateSize) {
            throw new RuntimeException("Subtemplate include aborted because the internal template string is longer than "+maxInclTemplateSize+" characters."); }
        String subtemplate;
        try {
            subtemplate = miniTemplator.loadSubtemplate(subtemplateName); }
        catch (IOException e) {
            throw new RuntimeException("Error while loading subtemplate \""+subtemplateName+"\"", e); }
        // (Copying the template to insert a subtemplate is a bit slow. In a future implementation of MiniTemplator,
        // a table could be used that contains references to the string fragments.)
        StringBuilder s = new StringBuilder(templateText.length()+subtemplate.length());
        s.append(templateText, 0, tPos1);
        s.append(subtemplate);
        s.append(templateText, tPos2, templateText.length());
        templateText = s.toString();
        resumeCmdParsingFromStart = true; }

//--- Conditional commands -----------------------------------------------------

    // Returns the enabled/disabled state of the condition at level condLevel2.
    private boolean isCondEnabled (int condLevel2) {
        if (condLevel2 < 0) {
            return true; }
        return condEnabled[condLevel2]; }

    // If the current condition is disabled, the text from tPosBegin to tPosEnd
// is excluded and true is returned.
// Otherwise nothing is done and false is returned.
    private boolean conditionalExclude (int tPosBegin, int tPosEnd) {
        if (isCondEnabled(condLevel)) {
            return false; }
        excludeTemplateRange(tPosBegin, tPosEnd);
        return true; }

    // Evaluates a condition expression of a conditional command, by comparing the
// flags in the expression with the flags in TemplateSpecification.conditionFlags.
// Returns true the condition is met.
    private boolean evaluateConditionFlags (String flags) {
        int p = 0;
        while (true) {
            p = skipBlanks(flags, p);
            if (p >= flags.length()) {
                break; }
            boolean complement = false;
            if (flags.charAt(p) == '!') {
                complement = true; p++; }
            p = skipBlanks(flags, p);
            if (p >= flags.length()) {
                break; }
            int p0 = p;
            p = skipNonBlanks(flags, p0+1);
            String flag = flags.substring(p0, p).toUpperCase();
            if ((conditionFlags != null && conditionFlags.contains(flag)) ^ complement) {
                return true; }}
        return false; }

    // Processes the $if command.
    private void processIfCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        excludeTemplateRange(cmdTPosBegin, cmdTPosEnd);
        if (condLevel >= maxCondLevels-1) {
            throw new MiniTemplator.TemplateSyntaxException ("Too many nested $if commands."); }
        condLevel++;
        boolean enabled = isCondEnabled(condLevel-1) && evaluateConditionFlags(parms);
        condEnabled[condLevel] = enabled;
        condPassed[condLevel] = enabled; }

    // Processes the $elseIf command.
    private void processElseIfCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        excludeTemplateRange(cmdTPosBegin, cmdTPosEnd);
        if (condLevel < 0) {
            throw new MiniTemplator.TemplateSyntaxException ("$elseIf without matching $if."); }
        boolean enabled = isCondEnabled(condLevel-1) && !condPassed[condLevel] && evaluateConditionFlags(parms);
        condEnabled[condLevel] = enabled;
        if (enabled) {
            condPassed[condLevel] = true; }}

    // Processes the $else command.
    private void processElseCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        excludeTemplateRange(cmdTPosBegin, cmdTPosEnd);
        if (parms.trim().length() != 0) {
            throw new MiniTemplator.TemplateSyntaxException ("Invalid parameters for $else command."); }
        if (condLevel < 0) {
            throw new MiniTemplator.TemplateSyntaxException ("$else without matching $if."); }
        boolean enabled = isCondEnabled(condLevel-1) && !condPassed[condLevel];
        condEnabled[condLevel] = enabled;
        if (enabled) {
            condPassed[condLevel] = true; }}

    // Processes the $endIf command.
    private void processEndIfCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        excludeTemplateRange(cmdTPosBegin, cmdTPosEnd);
        if (parms.trim().length() != 0) {
            throw new MiniTemplator.TemplateSyntaxException ("Invalid parameters for $endIf command."); }
        if (condLevel < 0) {
            throw new MiniTemplator.TemplateSyntaxException ("$endif without matching $if."); }
        condLevel--; }

//------------------------------------------------------------------------------

    // Associates variable references with blocks.
    private void associateVariablesWithBlocks() {
        int varRefNo = 0;
        int activeBlockNo = 0;
        int nextBlockNo = 1;
        while (varRefNo < varRefTabCnt) {
            VarRefTabRec vrtr = varRefTab[varRefNo];
            int varRefTPos = vrtr.tPosBegin;
            int varNo = vrtr.varNo;
            if (varRefTPos >= blockTab[activeBlockNo].tPosEnd) {
                activeBlockNo = blockTab[activeBlockNo].parentBlockNo;
                continue; }
            if (nextBlockNo < blockTabCnt && varRefTPos >= blockTab[nextBlockNo].tPosBegin) {
                activeBlockNo = nextBlockNo;
                nextBlockNo++;
                continue; }
            BlockTabRec btr = blockTab[activeBlockNo];
            if (varRefTPos < btr.tPosBegin) {
                throw new AssertionError(); }
            int blockVarNo = btr.blockVarCnt++;
            if (btr.blockVarCnt > btr.blockVarNoToVarNoMap.length) {
                btr.blockVarNoToVarNoMap = (int[])resizeArray(btr.blockVarNoToVarNoMap, 2*btr.blockVarCnt); }
            btr.blockVarNoToVarNoMap[blockVarNo] = varNo;
            if (btr.firstVarRefNo == -1) {
                btr.firstVarRefNo = varRefNo; }
            vrtr.blockNo = activeBlockNo;
            vrtr.blockVarNo = blockVarNo;
            varRefNo++; }}

    // Parses variable references within the template in the format "${VarName}" .
    private void parseTemplateVariables()
            throws MiniTemplator.TemplateSyntaxException {
        int p = 0;
        while (true) {
            p = templateText.indexOf("${", p);
            if (p == -1) {
                break; }
            int p0 = p;
            p = templateText.indexOf("}", p);
            if (p == -1) {
                throw new MiniTemplator.TemplateSyntaxException("Invalid variable reference in template at offset " + p0 + "."); }
            p++;
            String varName = templateText.substring(p0+2, p-1).trim();
            if (varName.length() == 0) {
                throw new MiniTemplator.TemplateSyntaxException("Empty variable name in template at offset " + p0 + "."); }
            registerVariableReference(varName, p0, p); }}

    private void registerVariableReference (String varName, int tPosBegin, int tPosEnd) {
        int varNo;
        varNo = lookupVariableName(varName);
        if (varNo == -1) {
            varNo = registerVariable(varName); }
        int varRefNo = varRefTabCnt++;
        if (varRefTabCnt > varRefTab.length) {
            varRefTab = (VarRefTabRec[])resizeArray(varRefTab, 2*varRefTabCnt); }
        VarRefTabRec vrtr = new VarRefTabRec();
        varRefTab[varRefNo] = vrtr;
        vrtr.tPosBegin = tPosBegin;
        vrtr.tPosEnd = tPosEnd;
        vrtr.varNo = varNo; }

    // Returns the variable number of the newly registered variable.
    private int registerVariable (String varName) {
        int varNo = varTabCnt++;
        if (varTabCnt > varTab.length) {
            varTab = (String[])resizeArray(varTab, 2*varTabCnt); }
        varTab[varNo] = varName;
        varNameToNoMap.put(varName.toUpperCase(), new Integer(varNo));
        return varNo; }

//--- name lookup routines -------------------------------------------

    // Maps variable name to variable number.
// Returns -1 if the variable name is not found.
    public int lookupVariableName (String varName) {
        Integer varNoWrapper = varNameToNoMap.get(varName.toUpperCase());
        if (varNoWrapper == null) {
            return -1; }
        int varNo = varNoWrapper.intValue();
        return varNo; }

    // Maps block name to block number.
// If there are multiple blocks with the same name, the block number of the last
// registered block with that name is returned.
// Returns -1 if the block name is not found.
    public int lookupBlockName (String blockName) {
        Integer blockNoWrapper = blockNameToNoMap.get(blockName.toUpperCase());
        if (blockNoWrapper == null) {
            return -1; }
        int blockNo = blockNoWrapper.intValue();
        return blockNo; }

//--- general utility routines ---------------------------------------

    // Reallocates an array with a new size and copies the contents
// of the old array to the new array.
    public static Object resizeArray (Object oldArray, int newSize) {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class<?> elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(
                elementType, newSize);
        int preserveLength = Math.min(oldSize, newSize);
        if (preserveLength > 0) {
            System.arraycopy(oldArray, 0, newArray, 0, preserveLength); }
        return newArray; }

    // Skips blanks (white space) in string s starting at position p.
    private static int skipBlanks (String s, int p) {
        while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++;
        return p; }

    // Skips non-blanks (no-white space) in string s starting at position p.
    private static int skipNonBlanks (String s, int p) {
        while (p < s.length() && !Character.isWhitespace(s.charAt(p))) p++;
        return p; }

    // Returns true if string s is blank (white space) from position p to the end.
    public static boolean isRestOfStringBlank (String s, int p) {
        return skipBlanks(s, p) >= s.length(); }

} // End class MiniTemplatorParser