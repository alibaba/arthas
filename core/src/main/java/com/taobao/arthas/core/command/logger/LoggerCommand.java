package com.taobao.arthas.core.command.logger;

import static com.taobao.text.ui.Element.label;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.common.ReflectUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

/**
 * logger command
 * 
 * @author hengyunabc 2019-09-04
 *
 */
//@formatter:off
@Name("logger")
@Summary("Print logger info, and update the logger level")
@Description("\nExamples:\n"
                + "  logger\n"
                + "  logger -c 327a647b\n"
                + "  logger -c 327a647b --name ROOT --level debug\n"
                + "  logger --include-no-appender\n"
                + Constants.WIKI + Constants.WIKI_HOME + "logger")
//@formatter:on
public class LoggerCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(LoggerCommand.class);

    private static byte[] LoggerHelperBytes;
    private static byte[] Log4jHelperBytes;
    private static byte[] LogbackHelperBytes;
    private static byte[] Log4j2HelperBytes;

    private static Map<Class<?>, byte[]> classToBytesMap = new HashMap<Class<?>, byte[]>();

    private static String arthasClassLoaderHash = ClassLoaderUtils
                    .classLoaderHash(LoggerCommand.class.getClassLoader());

    static {
        LoggerHelperBytes = loadClassBytes(LoggerHelper.class);
        Log4jHelperBytes = loadClassBytes(Log4jHelper.class);
        LogbackHelperBytes = loadClassBytes(LogbackHelper.class);
        Log4j2HelperBytes = loadClassBytes(Log4j2Helper.class);

        classToBytesMap.put(LoggerHelper.class, LoggerHelperBytes);
        classToBytesMap.put(Log4jHelper.class, Log4jHelperBytes);
        classToBytesMap.put(LogbackHelper.class, LogbackHelperBytes);
        classToBytesMap.put(Log4j2Helper.class, Log4j2HelperBytes);
    }

    private String name;

    private String hashCode;

    private String level;

    /**
     * include the loggers which don't have appenders, default false.
     */
    private boolean includeNoAppender;

    /**
     * include the arthas logger, default false.
     */
    private boolean includeArthasLogger;

    private Pattern classPattern;

    private int max  = -1;

    @Option(shortName = "m", longName = "max-loggers")
    @Description("max logger to display, default is -1")
    public void setMax(int max) {
      this.max = max;
    }

    @Option(shortName = "p", longName = "filter-pattern")
    @Description("classname pattern for find the classloader or filter logger")
    public void setClassPattern(String classPattern) {
      this.classPattern = classPattern  == null || classPattern.isEmpty() ? null : Pattern.compile(classPattern, Pattern.CASE_INSENSITIVE);
    }

    @Option(shortName = "n", longName = "name")
    @Description("logger name")
    public void setName(String name) {
        this.name = name;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("classLoader hashcode, if no value is set, default value is SystemClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(shortName = "l", longName = "level")
    @Description("set logger level")
    public void setLevel(String level) {
        this.level = level;
    }

    @Option(shortName = "a",  longName = "include-no-appender", flag = true)
    @Description("include the loggers which don't have appenders, default value false")
    public void setHaveAppender(boolean includeNoAppender) {
        this.includeNoAppender = includeNoAppender;
    }

    @Override
    public void process(CommandProcess process) {
        int status = 0;
        try {
            if (this.name != null && this.level != null) {
                level(process);
            } else {
                loggers(process, name);
            }
        } finally {
            process.end(status);
        }
    }

    public void level(CommandProcess process) {
        Instrumentation inst = process.session().getInstrumentation();
        boolean result = false;
        final LoggerTypes logTypes =  findLoggerTypes(process);
        final ClassLoader classloader = logTypes.classLoader;

        Set<LoggerType> types = logTypes.types;
        getLoggerInfoMap(classloader, logTypes);
        final Map<LoggerType,Boolean> updatOkTypes = new LinkedHashMap<LoggerType,Boolean>();
        if (types.size() < 1 || types.contains(LoggerType.LOG4J)) {
            try {
                Boolean updateResult = this.updateLevel(inst, Log4jHelper.class, classloader);
                if (Boolean.TRUE.equals(updateResult)) {
                    result = true;
                }
                updatOkTypes.put(LoggerType.LOG4J, updateResult);
            } catch (Throwable e) {
                logger.error("logger command update log4j level error", e);
            }
        }
        if (types.size() < 1 || types.contains(LoggerType.LOGBACK)) {
            try {
                Boolean updateResult = this.updateLevel(inst, LogbackHelper.class, classloader);
                if (Boolean.TRUE.equals(updateResult)) {
                    result = true;
                }
                updatOkTypes.put(LoggerType.LOGBACK, updateResult);
            } catch (Throwable e) {
                logger.error("logger command update logback level error", e);
            }
        }
        if (types.size() < 1 || types.contains(LoggerType.LOG4J2)) {
            try {
                Boolean updateResult = this.updateLevel(inst, Log4j2Helper.class, classloader);
                if (Boolean.TRUE.equals(updateResult)) {
                    result = true;
                }
                updatOkTypes.put(LoggerType.LOG4J2, updateResult);
            } catch (Throwable e) {
                logger.error("logger command update log4j2 level error", e);
            }
        }

        final String classLoaderHash = ClassLoaderUtils.classLoaderHash(classloader);
        if (result) {
            process.write("update logger level success." + updatOkTypes + " at classloader:" + classLoaderHash + "\n");
        } else {
            process.write("Update logger level fail. logTypes=" + types + " at classloader:" + classLoaderHash
              + "\nTry to specify the classloader with the -c option. Use `sc -d CLASSNAME` to find out the classloader hashcode.\n");
        }
    }

    public void loggers(CommandProcess process, String name) {
        Map<ClassLoader, LoggerTypes> classLoaderLoggerMap = new LinkedHashMap<ClassLoader, LoggerTypes>();

        for (Class<?> clazz : process.session().getInstrumentation().getAllLoadedClasses()) {
            String className = clazz.getName();
            ClassLoader classLoader = clazz.getClassLoader();
            // skip the arthas classloader
            if (this.includeArthasLogger == false && classLoader != null && this.getClass().getClassLoader().getClass()
                .getName().equals(classLoader.getClass().getName())) {
                continue;
            }
            // if special classloader
            if (this.hashCode != null && !this.hashCode.equals(StringUtils.classLoaderHash(clazz))) {
                continue;
            }

            if (classLoader != null) {
                LoggerTypes loggerTypes = classLoaderLoggerMap.get(classLoader);
                if (loggerTypes == null) {
                    loggerTypes = new LoggerTypes(classLoader);
                    classLoaderLoggerMap.put(classLoader, loggerTypes);
                }
                addLoggerTypes(className, loggerTypes);
            }
        }
        int count = 0 ;
        for (Entry<ClassLoader, LoggerTypes> entry : classLoaderLoggerMap.entrySet()) {
            ClassLoader classLoader = entry.getKey();
            LoggerTypes loggerTypes = entry.getValue();
            Map<String, Map<String, Object>> loggerInfoMap = getLoggerInfoMap(classLoader, loggerTypes);
            if (loggerInfoMap != null) {
              count  = renderLoggerInfo(loggerInfoMap, process, count);
            }
        }
    }


    protected LoggerTypes findLoggerTypes(CommandProcess process) {
      ClassLoader ret = null;
      final Instrumentation inst = process.session().getInstrumentation();
      ClassLoader matched = null;
      Map<ClassLoader, LoggerTypes> classLoaderLoggerMap = new LinkedHashMap<ClassLoader, LoggerTypes>();
      for (Class<?> clazz : inst.getAllLoadedClasses()) {
          String className = clazz.getName();
          // if special classloader
          ClassLoader classLoader = clazz.getClassLoader();
          // skip the arthas classloader
          if (this.includeArthasLogger == false && classLoader != null && this.getClass().getClassLoader().getClass()
                          .getName().equals(classLoader.getClass().getName())) {
              continue;
          }
          if (classLoader != null) {
              if (this.hashCode != null && !this.hashCode.equals(StringUtils.classLoaderHash(clazz))) {
                  continue;
              } else if(this.hashCode != null){
                  matched = classLoader;
              } else if (classPattern != null && classPattern.matcher(className).find()) {
                  matched = classLoader;
               // break;
              } else  if (name != null && className.contains(name)) {
                  matched = classLoader;
               // break;
              }

              LoggerTypes loggerTypes = classLoaderLoggerMap.get(classLoader);
              if (loggerTypes == null) {
                  loggerTypes = new LoggerTypes(classLoader);
                  classLoaderLoggerMap.put(classLoader, loggerTypes);
              }
              if (addLoggerTypes(className, loggerTypes)) {
                  ret = classLoader;
              }
           }
      }

      LoggerTypes loggerTypes = matched != null  ?  classLoaderLoggerMap.get(matched) : null;
      if (loggerTypes != null && loggerTypes.types().size()>0) {
          ret = matched;
      }
      if(ret != matched && ret != null) {
          loggerTypes = classLoaderLoggerMap.get(ret);
      }
      //logger.info("classloader:{} {}", ClassLoaderUtils.classLoaderHash(ret), ret);
      return ret == null ? new LoggerTypes(ClassLoader.getSystemClassLoader()): loggerTypes;
  }

    protected boolean addLoggerTypes(String className, LoggerTypes loggerTypes) {
      if ("org.apache.log4j.Logger".equals(className)) {
          loggerTypes.addType(LoggerType.LOG4J);
      } else if ("ch.qos.logback.classic.Logger".equals(className)) {
          loggerTypes.addType(LoggerType.LOGBACK);
      } else if ("org.apache.logging.log4j.Logger".equals(className)) {
          loggerTypes.addType(LoggerType.LOG4J2);
      } else {
        return false;
      }
      return true;
    }

    protected Map<String, Map<String, Object>> getLoggerInfoMap(ClassLoader classLoader,
        LoggerTypes loggerTypes) {
      Map<String, Map<String, Object>> loggerInfoMap = null;
      if (loggerTypes.contains(LoggerType.LOG4J)) {
          loggerInfoMap = loggerInfo(classLoader, Log4jHelper.class);
        }

      if (loggerTypes.contains(LoggerType.LOGBACK)) {
          loggerInfoMap = loggerInfo(classLoader, LogbackHelper.class);
      }

      if (loggerTypes.contains(LoggerType.LOG4J2)) {
          loggerInfoMap = loggerInfo(classLoader, Log4j2Helper.class);
      }
      return loggerInfoMap;
    }

    private int renderLoggerInfo( Map<String, Map<String, Object>> loggerInfos, CommandProcess process,int loggerCount) {
        StringBuilder sb = new StringBuilder(8192);
        final int width = process.width();
        for (Entry<String, Map<String, Object>> entry : loggerInfos.entrySet()) {
            Map<String, Object> info = entry.getValue();

            TableElement table = new TableElement(2, 10).leftCellPadding(1).rightCellPadding(1);
            TableElement appendersTable = new TableElement().rightCellPadding(1);

            Class<?> clazz = (Class<?>) info.get(LoggerHelper.clazz);
            final String loggerName = "" + info.get(LoggerHelper.name);
            if (classPattern != null && !classPattern.matcher(loggerName).find()) {
              continue;
            }
            loggerCount ++;
            table.row(label(LoggerHelper.name).style(Decoration.bold.bold()), label(loggerName))
                            .row(label(LoggerHelper.clazz).style(Decoration.bold.bold()), label("" + clazz.getName()))
                            .row(label(LoggerHelper.classLoader).style(Decoration.bold.bold()),
                                            label("" + clazz.getClassLoader()))
                            .row(label(LoggerHelper.classLoaderHash).style(Decoration.bold.bold()),
                                            label("" + StringUtils.classLoaderHash(clazz)))
                            .row(label(LoggerHelper.level).style(Decoration.bold.bold()),
                                            label("" + info.get(LoggerHelper.level)));
            if (info.get(LoggerHelper.effectiveLevel) != null) {
                table.row(label(LoggerHelper.effectiveLevel).style(Decoration.bold.bold()),
                                label("" + info.get(LoggerHelper.effectiveLevel)));
            }

            if (info.get(LoggerHelper.config) != null) {
                table.row(label(LoggerHelper.config).style(Decoration.bold.bold()),
                                label("" + info.get(LoggerHelper.config)));
            }

            table.row(label(LoggerHelper.additivity).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.additivity)))
                            .row(label(LoggerHelper.codeSource).style(Decoration.bold.bold()),
                                            label("" + info.get(LoggerHelper.codeSource)));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> appenders = (List<Map<String, Object>>) info.get(LoggerHelper.appenders);
            if (appenders != null && !appenders.isEmpty()) {

                for (Map<String, Object> appenderInfo : appenders) {
                    Class<?> appenderClass = (Class<?>) appenderInfo.get(LoggerHelper.clazz);

                    appendersTable.row(label(LoggerHelper.name).style(Decoration.bold.bold()),
                                    label("" + appenderInfo.get(LoggerHelper.name)));
                    appendersTable.row(label(LoggerHelper.clazz), label("" + appenderClass.getName()));
                    appendersTable.row(label(LoggerHelper.classLoader), label("" + appenderClass.getClassLoader()));
                    appendersTable.row(label(LoggerHelper.classLoaderHash),
                                    label("" + StringUtils.classLoaderHash(appenderClass)));
                    if (appenderInfo.get(LoggerHelper.file) != null) {
                        appendersTable.row(label(LoggerHelper.file), label("" + appenderInfo.get(LoggerHelper.file)));
                    }
                    if (appenderInfo.get(LoggerHelper.target) != null) {
                        appendersTable.row(label(LoggerHelper.target),
                                        label("" + appenderInfo.get(LoggerHelper.target)));
                    }
                    if (appenderInfo.get(LoggerHelper.blocking) != null) {
                        appendersTable.row(label(LoggerHelper.blocking),
                                        label("" + appenderInfo.get(LoggerHelper.blocking)));
                    }
                    if (appenderInfo.get(LoggerHelper.appenderRef) != null) {
                        appendersTable.row(label(LoggerHelper.appenderRef),
                                        label("" + appenderInfo.get(LoggerHelper.appenderRef)));
                    }
                }

                table.row(label("appenders").style(Decoration.bold.bold()), appendersTable);
            }

            sb.append(RenderUtil.render(table, width)).append('\n');
            if (max > 0  && loggerCount > max) {
                process.write("....over max:" + max + "\n");
                break;
            }
        }
        process.write(sb.toString());
        return loggerCount;
    }

    private static String helperClassNameWithClassLoader(ClassLoader classLoader, Class<?> helperClass) {
        String classLoaderHash = ClassLoaderUtils.classLoaderHash(classLoader);
        String className = helperClass.getName();
        // if want to debug, change to return className
        return className + arthasClassLoaderHash + classLoaderHash;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> loggerInfo(ClassLoader classLoader, Class<?> helperClass) {
        Map<String, Map<String, Object>> loggers = Collections.emptyMap();

        String helperClassName = helperClassNameWithClassLoader(classLoader, helperClass);
        try {
            classLoader.loadClass(helperClassName);
        } catch (ClassNotFoundException e) {
            try {
                byte[] helperClassBytes = AsmRenameUtil.renameClass(classToBytesMap.get(helperClass),
                                helperClass.getName(), helperClassName);
                ReflectUtils.defineClass(helperClassName, helperClassBytes, classLoader);
            } catch (Throwable e1) {
                logger.error("arthas loggger command try to define helper class error: " + helperClassName,
                                e1);
            }
        }

        try {
            Class<?> clazz = classLoader.loadClass(helperClassName);
            Method getLoggersMethod = clazz.getMethod("getLoggers", new Class<?>[] { String.class, boolean.class });
            loggers = (Map<String, Map<String, Object>>) getLoggersMethod.invoke(null,
                            new Object[] { name, includeNoAppender });
        } catch (Throwable e) {
            // ignore
        }
        return loggers;
    }

    private Boolean updateLevel(Instrumentation inst, Class<?> helperClass,  ClassLoader classLoader) throws Exception {
        Class<?> clazz = classLoader.loadClass(helperClassNameWithClassLoader(classLoader, helperClass));
        Method updateLevelMethod = clazz.getMethod("updateLevel", new Class<?>[] { String.class, String.class });
        return (Boolean) updateLevelMethod.invoke(null, new Object[] { this.name, this.level });

    }

    static enum LoggerType {
        LOG4J, LOGBACK, LOG4J2
    }

    static class LoggerTypes {
        Set<LoggerType> types = new HashSet<LoggerType>();
        final ClassLoader classLoader;

        public LoggerTypes(ClassLoader classLoader) {
            super();
            this.classLoader = classLoader;
        }

        public Collection<LoggerType> types() {
            return types;
        }

        public void addType(LoggerType type) {
            types.add(type);
        }

        public boolean contains(LoggerType type) {
            return types.contains(type);
        }
    }

    private static byte[] loadClassBytes(Class<?> clazz) {
        try {
            InputStream stream = LoggerCommand.class.getClassLoader()
                            .getResourceAsStream(clazz.getName().replace('.', '/') + ".class");

            return IOUtils.getBytes(stream);
        } catch (IOException e) {
            // ignore
            return null;
        }
    }
}
