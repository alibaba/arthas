package com.taobao.arthas.core.command;

import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.core.command.basic1000.CatCommand;
import com.taobao.arthas.core.command.basic1000.ClsCommand;
import com.taobao.arthas.core.command.basic1000.EchoCommand;
import com.taobao.arthas.core.command.basic1000.GrepCommand;
import com.taobao.arthas.core.command.basic1000.HelpCommand;
import com.taobao.arthas.core.command.basic1000.HistoryCommand;
import com.taobao.arthas.core.command.basic1000.KeymapCommand;
import com.taobao.arthas.core.command.basic1000.OptionsCommand;
import com.taobao.arthas.core.command.basic1000.PwdCommand;
import com.taobao.arthas.core.command.basic1000.ResetCommand;
import com.taobao.arthas.core.command.basic1000.SessionCommand;
import com.taobao.arthas.core.command.basic1000.ShutdownCommand;
import com.taobao.arthas.core.command.basic1000.StopCommand;
import com.taobao.arthas.core.command.basic1000.SystemEnvCommand;
import com.taobao.arthas.core.command.basic1000.SystemPropertyCommand;
import com.taobao.arthas.core.command.basic1000.TeeCommand;
import com.taobao.arthas.core.command.basic1000.VMOptionCommand;
import com.taobao.arthas.core.command.basic1000.VersionCommand;
import com.taobao.arthas.core.command.hidden.JulyCommand;
import com.taobao.arthas.core.command.hidden.ThanksCommand;
import com.taobao.arthas.core.command.klass100.ClassLoaderCommand;
import com.taobao.arthas.core.command.klass100.DumpClassCommand;
import com.taobao.arthas.core.command.klass100.GetStaticCommand;
import com.taobao.arthas.core.command.klass100.JadCommand;
import com.taobao.arthas.core.command.klass100.MemoryCompilerCommand;
import com.taobao.arthas.core.command.klass100.OgnlCommand;
import com.taobao.arthas.core.command.klass100.RedefineCommand;
import com.taobao.arthas.core.command.klass100.SearchClassCommand;
import com.taobao.arthas.core.command.klass100.SearchMethodCommand;
import com.taobao.arthas.core.command.logger.LoggerCommand;
import com.taobao.arthas.core.command.monitor200.DashboardCommand;
import com.taobao.arthas.core.command.monitor200.HeapDumpCommand;
import com.taobao.arthas.core.command.monitor200.JvmCommand;
import com.taobao.arthas.core.command.monitor200.MBeanCommand;
import com.taobao.arthas.core.command.monitor200.MonitorCommand;
import com.taobao.arthas.core.command.monitor200.PerfCounterCommand;
import com.taobao.arthas.core.command.monitor200.ProfilerCommand;
import com.taobao.arthas.core.command.monitor200.StackCommand;
import com.taobao.arthas.core.command.monitor200.ThreadCommand;
import com.taobao.arthas.core.command.monitor200.TimeTunnelCommand;
import com.taobao.arthas.core.command.monitor200.TraceCommand;
import com.taobao.arthas.core.command.monitor200.WatchCommand;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import io.netty.util.internal.ResourcesUtil;

import java.io.*;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * TODO automatically discover the built-in commands.
 *
 * @author beiwei30 on 17/11/2016.
 */
public class BuiltinCommandPack implements CommandResolver {

    private static List<Command> commands = new ArrayList<Command>();

    static {
        try {
            initCommands();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<Command> commands() {
        return commands;
    }

    private static void initCommands() throws IOException {
        for (Class<? extends AnnotatedCommand> commandClazz : getClasses(BuiltinCommandPack.class.getPackage().getName())) {
            try {
                commands.add(Command.create(commandClazz));
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Find all command classes from basePackageName
     *
     * @return
     */
    private static List<Class<? extends AnnotatedCommand>> getClasses(String basePackageName) throws IOException {

        List<Class<? extends AnnotatedCommand>> classes = new ArrayList<Class<? extends AnnotatedCommand>>();
        // replace base package name to file path
        String packageDirName = basePackageName.replace('.', '/');
        Enumeration<URL> dirs = BuiltinCommandPack.class.getClassLoader().getResources(packageDirName);
        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            String protocol = url.getProtocol();
            if ("jar".equals(protocol)) {
                // jar
                findAndAddClassesInPackageByJar(url, basePackageName, classes);
            } else {
                // file
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                findAndAddClassesInPackageByFile(basePackageName, filePath, classes);
            }
        }
        return classes;
    }

    private static void findAndAddClassesInPackageByJar(URL url, String basePackageName, List<Class<? extends AnnotatedCommand>> classes) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        if (connection == null) {
            throw new RuntimeException("Can't get JarURL connection : " + url.toString());
        }
        JarFile jarFile = connection.getJarFile();
        if (jarFile == null) {
            throw new RuntimeException("jarFile is null!");
        }
        Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
        while (jarEntryEnumeration.hasMoreElements()) {
            JarEntry entry = jarEntryEnumeration.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String clazzName = entry.getName().replace("/", ".");
            if (!clazzName.startsWith(basePackageName)) {
                continue;
            }
            addCommandToList(clazzName, classes);
        }
    }

    private static void findAndAddClassesInPackageByFile(String basePackageName, String packagePath,
                                                         List<Class<? extends AnnotatedCommand>> classes) {
        File[] dirfiles = null;
        File dir = new File(packagePath);
        if (!dir.exists()) {
            return;
        }
        if (!dir.isDirectory()) {
            return;
        }
        dirfiles = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || (file.getName().endsWith(".class") && !file.getName().endsWith("Test.class"));
            }
        });
        if (dirfiles == null || dirfiles.length == 0) {
            return;
        }
        // loop all files
        for (File file : dirfiles) {
            // if is directory do recursion
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(basePackageName + "." + file.getName(), file.getAbsolutePath(), classes);
            } else {
                // get the class name
                addCommandToList(basePackageName + '.' + file.getName(), classes);
            }
        }
    }

    private static void addCommandToList(String classNameWithClassSuffix, List<Class<? extends AnnotatedCommand>> classes) {
        String className = classNameWithClassSuffix.substring(0, classNameWithClassSuffix.length() - 6);
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
            if (isCommand(clazz)) {
                // if is command add to the list
                classes.add((Class<? extends AnnotatedCommand>) clazz);
            }
        } catch (ClassNotFoundException e) {}
    }

    /**
     * Meet the parent class is AnnotatedCommand
     * @param clazz
     * @return
     */
    private static boolean isCommand(Class<?> clazz) {
        Deprecated deprecated = clazz.getAnnotation(Deprecated.class);
        return deprecated == null && AnnotatedCommand.class.isAssignableFrom(clazz);
    }
}
