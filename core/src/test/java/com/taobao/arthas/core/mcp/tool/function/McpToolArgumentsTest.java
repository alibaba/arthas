package com.taobao.arthas.core.mcp.tool.function;

import com.taobao.arthas.core.command.klass100.*;
import com.taobao.arthas.core.command.monitor200.*;
import com.taobao.arthas.core.mcp.tool.function.jvm300.*;
import com.taobao.arthas.core.mcp.tool.function.klass100.*;
import com.taobao.arthas.core.mcp.tool.function.monitor200.*;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.tool.ToolContext;
import com.taobao.arthas.mcp.server.tool.ToolContextKeys;
import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class McpToolArgumentsTest {

    @Test
    public void monitorToolsShouldPreservePatternsAndConditions() throws Exception {
        String classPattern = "demo\\.Outer\\$Inner";
        String methodPattern = "hot\\w+";
        String condition = "params[0] == 'ok' && params.length > 0";
        for (Object[] entry : monitorTools()) {
            CommandLine command = parse((Class<?>) entry[1], call(entry[0],
                    "classPattern", classPattern, "methodPattern", methodPattern,
                    "regex", true, "condition", condition));
            assertThat((String) command.getArgumentValue("class-pattern")).isEqualTo(classPattern);
            assertThat((String) command.getArgumentValue("method-pattern")).isEqualTo(methodPattern);
            assertThat((String) command.getArgumentValue("condition-express")).isEqualTo(condition);
        }
    }

    @Test
    public void omittedMethodShouldNotConsumeTheExpression() throws Exception {
        for (Object[] entry : monitorTools()) {
            for (boolean regex : new boolean[] { false, true }) {
                CommandLine command = parse((Class<?>) entry[1], call(entry[0],
                        "classPattern", "demo.MathGame", "regex", regex, "condition", "params.length > 0"));
                assertThat((String) command.getArgumentValue("method-pattern")).isEqualTo(regex ? ".*" : "*");
                assertThat((String) command.getArgumentValue("condition-express")).isEqualTo("params.length > 0");
                if (entry[0] instanceof WatchTool) {
                    assertThat((String) command.getArgumentValue("express")).isEqualTo(WatchTool.DEFAULT_EXPRESS);
                }
            }
        }
    }

    @Test
    public void watchAndTimeTunnelShouldPreserveSingleQuotes() throws Exception {
        String express = "params[0]['name']";
        CommandLine watch = parse(WatchCommand.class, call(new WatchTool(),
                "classPattern", "demo.MathGame", "express", express));
        assertThat((String) watch.getArgumentValue("express")).isEqualTo(express);

        String condition = "params[0] == 'ok'";
        CommandLine record = parse(TimeTunnelCommand.class, call(new TimeTunnelTool(),
                "action", "record", "classPattern", "demo.MathGame", "methodPattern", "hot\\w+",
                "regex", true, "condition", condition));
        assertThat((String) record.getArgumentValue("condition-express")).isEqualTo(condition);
        assertThat((String) record.getArgumentValue("method-pattern")).isEqualTo("hot\\w+");
        String search = "method.name == 'hotMethod'";
        CommandLine query = parse(TimeTunnelCommand.class, call(new TimeTunnelTool(),
                "action", "search", "searchExpression", search));
        assertThat((String) query.getOptionValue("s")).isEqualTo(search);
    }

    @Test
    public void lineShouldPreserveProbeAndListArguments() throws Exception {
        String classPattern = "demo\\.Outer\\$Inner";
        String methodPattern = "hot\\w+";
        String descriptor = "(Ljava/lang/String;)V";
        for (boolean listLines : new boolean[] { false, true }) {
            CommandLine line = parse(LineCommand.class, call(new LineTool(),
                    "classPattern", classPattern, "methodPattern", methodPattern,
                    "methodDesc", descriptor, "regex", true, "listLines", listLines,
                    "line", "51, 57", "express", "localVarMap['count']", "condition", "params[0] == 'ok'"));
            assertThat((String) line.getOptionValue("class")).isEqualTo(classPattern);
            assertThat((String) line.getOptionValue("method")).isEqualTo(methodPattern);
            assertThat((String) line.getOptionValue("desc")).isEqualTo(descriptor);
            if (!listLines) {
                assertThat((String) line.getOptionValue("express")).isEqualTo("localVarMap['count']");
                assertThat((String) line.getOptionValue("condition")).isEqualTo("params[0] == 'ok'");
                assertThat(line.<String>getOptionValues("line")).containsExactly("51, 57");
            }
        }
    }

    @Test
    public void expressionsShouldRemainOneArgumentWithoutLosingQuotes() throws Exception {
        String expression = "instances[0].equals(\"O'Reilly\") && instances.length > 0";
        CommandLine vmtool = parse(VmToolCommand.class, call(new VMToolTool(),
                "action", "getInstances", "className", "java.lang.String", "express", expression));
        assertThat((String) vmtool.getOptionValue("express")).isEqualTo(expression);

        String getstaticExpression = "get(\"name\") == 'ok'";
        CommandLine getstatic = parse(GetStaticCommand.class, call(new GetStaticTool(),
                "className", "demo.Config", "fieldName", "values", "ognlExpression", getstaticExpression));
        assertThat((String) getstatic.getArgumentValue("express")).isEqualTo(getstaticExpression);
        assertThat(call(new OgnlTool(), "expression", getstaticExpression))
                .containsExactly("ognl", getstaticExpression);
    }

    @Test
    public void namesAndFiltersShouldPreserveWhitespaceAndBackslashes() throws Exception {
        String objectName = "java.lang:type=MemoryPool,name=G1 Eden Space";
        CommandLine mbean = parse(MBeanCommand.class, call(new MBeanTool(),
                "namePattern", objectName, "attributePattern", "Collection\\w+", "regex", true));
        assertThat((String) mbean.getArgumentValue("name-pattern")).isEqualTo(objectName);
        assertThat((String) mbean.getArgumentValue("attribute-pattern")).isEqualTo("Collection\\w+");

        CommandLine loader = parse(ClassLoaderCommand.class, call(new ClassLoaderTool(),
                "mode", "url-classes", "regex", true, "jar", "app-\\d+\\.jar", "classFilter", "demo\\.Foo"));
        assertThat((String) loader.getOptionValue("jar")).isEqualTo("app-\\d+\\.jar");
        assertThat((String) loader.getOptionValue("class")).isEqualTo("demo\\.Foo");
        assertThat(call(new ClassLoaderTool(), "resource", "config/app settings.xml"))
                .containsExactly("classloader", "-r", "config/app settings.xml");
        CommandLine sc = parse(SearchClassCommand.class, call(new SearchClassTool(),
                "classPattern", "demo.*", "classLoaderStr", "App Loader"));
        assertThat((String) sc.getOptionValue("cs")).isEqualTo("App Loader");
        assertThat((String) sc.getArgumentValue("class-pattern")).isEqualTo("demo.*");
        assertThat(call(new SearchMethodTool(), "classPattern", "demo\\.Foo",
                "methodPattern", "hot\\w+", "regex", true)).contains("demo\\.Foo", "hot\\w+");
    }

    @Test
    public void pathsAndPropertyValuesShouldRemainIntact() throws Exception {
        String directory = "/tmp/O'Reilly output";
        assertThat(call(new DumpClassTool(), "classPattern", "demo.MathGame", "outputDir", directory))
                .containsSubsequence("-d", directory);
        assertThat(call(new JadTool(), "classPattern", "demo.MathGame", "dumpDirectory", directory))
                .containsSubsequence("-d", directory);
        assertThat(call(new MemoryCompilerTool(), "javaFilePaths", "/tmp/A.java", "outputDir", directory))
                .containsSubsequence("-d", directory);
        assertThat(call(new HeapdumpTool(), "filePath", directory + "/heap.hprof"))
                .containsExactly("heapdump", directory + "/heap.hprof");
        assertThat(call(new ProfilerTool(), "action", "start", "file", directory + "/cpu.html"))
                .containsSubsequence("--file", directory + "/cpu.html");
        assertThat(call(new VMOptionTool(), "key", "HeapDumpPath", "value", directory))
                .containsExactly("vmoption", "HeapDumpPath", directory);
        for (String value : new String[] { "hello world", "C:\\temp\\app", "  O'Reilly  " }) {
            assertThat(call(new SysPropTool(), "propertyName", "audit.value", "propertyValue", value))
                    .containsExactly("sysprop", "audit.value", value);
        }
    }

    @Test
    public void fileListsShouldProduceSeparatePathsIncludingQuotedAndWindowsPaths() throws Exception {
        for (Object[] entry : fileTools()) {
            String suffix = entry[0] instanceof MemoryCompilerTool ? ".java" : ".class";
            String first = "/tmp/dir with spaces/A" + suffix;
            String second = "C:\\temp\\B" + suffix;
            String third = "/tmp/O'Reilly/C" + suffix;
            String paths = "'" + first + "' " + second + " \"" + third + "\"";
            CommandLine command = parse((Class<?>) entry[1], call(entry[0], entry[2], paths));
            assertThat(command.<String>getArgumentValues(0)).containsExactly(first, second, third);
            CommandLine single = parse((Class<?>) entry[1], call(entry[0], entry[2], second));
            assertThat(single.<String>getArgumentValues(0)).containsExactly(second);
        }
    }

    @Test
    public void malformedFileListsShouldBeRejectedBeforeExecution() {
        for (String paths : new String[] { "'/tmp/A.java", "\"/tmp/A.java", "''", "'A.java'B.java" }) {
            assertThatThrownBy(() -> call(new MemoryCompilerTool(), "javaFilePaths", paths))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static Object[][] monitorTools() {
        return new Object[][] { { new WatchTool(), WatchCommand.class }, { new TraceTool(), TraceCommand.class },
                { new StackTool(), StackCommand.class }, { new MonitorTool(), MonitorCommand.class } };
    }

    private static Object[][] fileTools() {
        return new Object[][] { { new MemoryCompilerTool(), MemoryCompilerCommand.class, "javaFilePaths" },
                { new RedefineTool(), RedefineCommand.class, "classFilePaths" },
                { new RetransformTool(), RetransformCommand.class, "classFilePaths" } };
    }

    private static CommandLine parse(Class<?> commandType, List<String> tokens) {
        return CLIConfigurator.define(commandType, true).parse(tokens.subList(1, tokens.size()), true);
    }

    // Capture the real tool's command at the executor boundary; never run a diagnostic command.
    private static List<String> call(Object tool, Object... namedArguments) throws Exception {
        Map<String, Object> arguments = new HashMap<String, Object>();
        for (int i = 0; i < namedArguments.length; i += 2) {
            arguments.put((String) namedArguments[i], namedArguments[i + 1]);
        }
        ArthasCommandContext commandContext = mock(ArthasCommandContext.class);
        AtomicReference<String> captured = new AtomicReference<String>();
        when(commandContext.executeSync(anyString(), isNull(), isNull())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return Collections.singletonMap("success", true);
        });
        when(commandContext.executeAsync(anyString())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return Collections.singletonMap("success", false);
        });
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(ToolContextKeys.COMMAND_CONTEXT, commandContext);
        for (Method method : tool.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(Tool.class) == null) {
                continue;
            }
            Parameter[] parameters = method.getParameters();
            Object[] values = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                values[i] = parameters[i].getType() == ToolContext.class ? new ToolContext(context)
                        : arguments.remove(parameters[i].getName());
            }
            assertThat(arguments).as("unknown tool arguments").isEmpty();
            try {
                method.invoke(tool, values);
            } catch (InvocationTargetException e) {
                throw (Exception) e.getCause();
            }
            break;
        }
        assertThat(captured.get()).as("command sent by %s", tool.getClass().getSimpleName()).isNotNull();
        List<String> result = new ArrayList<String>();
        for (CliToken token : CliTokens.tokenize(captured.get())) {
            if (token.isText()) {
                result.add(token.value());
            }
        }
        return result;
    }
}
