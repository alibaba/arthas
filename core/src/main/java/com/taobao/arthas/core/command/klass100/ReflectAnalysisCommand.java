package com.taobao.arthas.core.command.klass100;


import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassVisitor;
import com.alibaba.deps.org.objectweb.asm.MethodVisitor;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.EchoModel;
import com.taobao.arthas.core.command.model.ReflectAnalysisModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.CommandUtils;
import com.taobao.arthas.core.util.InstrumentationUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.collection.MapUtil;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.deps.org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static com.alibaba.deps.org.objectweb.asm.ClassReader.SKIP_FRAMES;

/**
 * @author: Ares
 * @time: 2023-12-19 17:33:14
 * @description: 反射分析
 * @version: JDK 1.8
 */
@Name("reflect-analysis")
@Summary("Analyze the reflection situation within the application")
@Description(Constants.EXAMPLE +
    "  reflect-analysis\n" +
    "  reflect-analysis reflect-analysis-result.csv\n" +
    Constants.WIKI + Constants.WIKI_HOME + "dump")
public class ReflectAnalysisCommand extends AnnotatedCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReflectAnalysisCommand.class);
  private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

  @Override
  public void process(CommandProcess process) {
    List<String> args = process.args();

    if (args.isEmpty()) {
      ExitStatus exitStatus = processImpl(process, null);
      CommandUtils.end(process, exitStatus);
      return;
    }

    String resultFilePath = buildResultFilePath(args);
    CompletableFuture.runAsync(() -> processImpl(process, resultFilePath));
    String message = String.format("The reflect analysis result is being generated asynchronously, check the %s file later", resultFilePath);
    process.appendResult(new EchoModel(message));
    CommandUtils.end(process, ExitStatus.success());
  }

  private String buildResultFilePath(List<String> args) {
    String resultFilePath = args.get(0);
    Path path = Paths.get(resultFilePath);
    if (!path.isAbsolute()) {
      resultFilePath = TEMP_DIR + resultFilePath;
    }
    return resultFilePath;
  }

  private ExitStatus processImpl(CommandProcess process, String resultFilePath) {
    ExitStatus status;
    try {
      Instrumentation inst = process.session().getInstrumentation();
      // 找到所有反射生成的sun.reflect.GeneratedMethodAccessor类
      Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, "sun.reflect.GeneratedMethodAccessor*", false);
      if (null != matchedClasses && !matchedClasses.isEmpty()) {
        ClassDumpTransformer transformer = new ClassDumpTransformer(matchedClasses);
        InstrumentationUtils.retransformClasses(inst, transformer, matchedClasses);
        Map<Class<?>, File> dumpResult = transformer.getDumpResult();

        Map<String, List<String>> result = MapUtil.newHashMap(dumpResult.size());

        for (Map.Entry<Class<?>, File> entry : dumpResult.entrySet()) {
          Class<?> clazz = entry.getKey();
          File classFile = entry.getValue();

          String generatedMethodAccessorName = clazz.getSimpleName();
          try (FileInputStream inputStream = new FileInputStream(classFile)) {
            ClassReader classReader = new ClassReader(inputStream);
            ReflectAnalyzerClassVisitor classVisitor = new ReflectAnalyzerClassVisitor();
            classReader.accept(classVisitor, SKIP_DEBUG | SKIP_FRAMES);

            String invokeClassName = classVisitor.getInvokeClassName();
            String refName = classVisitor.getRefName();

            if (invokeClassName != null && refName != null) {
              String key = invokeClassName + "#" + refName;
              result.computeIfAbsent(key, k -> new ArrayList<>()).add(generatedMethodAccessorName);
            }
          } catch (Throwable throwable) {
            LOGGER.warn("analyze class file: {} fail:", classFile.getName(), throwable);
          }
        }

        processResult(process, result, resultFilePath);
      }
      status = ExitStatus.success();
    } catch (Throwable throwable) {
      LOGGER.error("processing fail:", throwable);
      process.end(-1, "processing fail");
      status = ExitStatus.failure(-1, "reflect analysis fail");
    }
    return status;
  }

  private void processResult(CommandProcess process, Map<String, List<String>> result, String resultFilePath) {
    List<ReflectAnalysisModel> resultList = new ArrayList<>(result.size());
    result.forEach((key, valueList) -> {
      ReflectAnalysisModel reflectAnalysisModel = new ReflectAnalysisModel();
      reflectAnalysisModel.setRefName(key);
      reflectAnalysisModel.setGeneratedMethodAccessorNameCount(valueList.size());
      StringBuilder generatedMethodAccessorNames = new StringBuilder();
      for (String generatedMethodAccessorName : valueList) {
        generatedMethodAccessorNames.append(generatedMethodAccessorName).append(",");
      }
      reflectAnalysisModel.setGeneratedMethodAccessorNames(generatedMethodAccessorNames.substring(0, generatedMethodAccessorNames.length() - 1));
      resultList.add(reflectAnalysisModel);
    });
    resultList.sort((leftMode, rightModel) -> {
      int leftCount = leftMode.getGeneratedMethodAccessorNameCount();
      int rightCount = rightModel.getGeneratedMethodAccessorNameCount();
      if (leftCount > rightCount) {
        return -1;
      } else if (leftCount < rightCount) {
        return 1;
      } else {
        return leftMode.getRefName().compareTo(rightModel.getRefName());
      }
    });
    if (null == resultFilePath) {
      for (ReflectAnalysisModel reflectAnalysisModel : resultList) {
        process.appendResult(reflectAnalysisModel);
      }
    } else {
      exportDataToCsvFile(resultList, resultFilePath);
    }
  }

  private void exportDataToCsvFile(List<ReflectAnalysisModel> reflectAnalysisModelList, String resultFilePath) {
    LOGGER.info("start write reflect analysis result: {} to file: {}", reflectAnalysisModelList.size(), resultFilePath);
    File file = new File(resultFilePath);
    try {
      if (file.exists()) {
        file.delete();
      }
      file.createNewFile();
      // write csv file
      try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
        bufferedWriter.write("count");
        bufferedWriter.write(",");
        bufferedWriter.write("refName");
        bufferedWriter.write(",");
        bufferedWriter.write("names");
        bufferedWriter.newLine();
        for (ReflectAnalysisModel reflectAnalysisModel : reflectAnalysisModelList) {
          bufferedWriter.write(String.valueOf(reflectAnalysisModel.getGeneratedMethodAccessorNameCount()));
          bufferedWriter.write(",");
          bufferedWriter.write(reflectAnalysisModel.getRefName());
          bufferedWriter.write(",");
          bufferedWriter.write(reflectAnalysisModel.getGeneratedMethodAccessorNames());
          bufferedWriter.newLine();
        }
        LOGGER.info("write csv file end");
      }
    } catch (Throwable throwable) {
      LOGGER.error("export data to csv file fail:", throwable);
    }
  }

  /**
   * ASM字节码分析器，用于直接从字节码中提取反射调用信息
   */
  private static class ReflectAnalyzerClassVisitor extends ClassVisitor {

    private String invokeClassName;
    private String refName;

    public ReflectAnalyzerClassVisitor() {
      super(Opcodes.ASM9);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      if ("invoke".equals(name)) {
        return new InvokeMethodVisitor();
      }
      return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    private class InvokeMethodVisitor extends MethodVisitor {

      public InvokeMethodVisitor() {
        super(Opcodes.ASM9);
      }

      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // 查找invoke方法中的方法调用，这通常是反射的目标方法
        if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL
            || opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKEINTERFACE) {

          // 过滤掉Java标准库和sun包的调用
          if (!owner.startsWith("java/") && !owner.startsWith("sun/") && !owner.startsWith("jdk/")) {
            invokeClassName = owner.replace('/', '.');
            refName = name;
          }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }
    }

    public String getInvokeClassName() {
      return invokeClassName;
    }

    public String getRefName() {
      return refName;
    }
  }

}
