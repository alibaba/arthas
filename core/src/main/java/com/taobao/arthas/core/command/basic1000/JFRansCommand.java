package com.taobao.arthas.core.command.basic1000;


import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import java.io.File;

@Name("jfr-analyze")
@Summary("启动 JFR 分析服务")
@Description("运行一个 HTTP 服务用于 .jfr 文件上传和火焰图展示")
public class JFRansCommand extends AnnotatedCommand {

    private static volatile boolean isStarted = false;

    @Override
    public void process(CommandProcess process) {
        try {
            if (isStarted) {
                process.write("服务已运行，访问 http://localhost:8200\n");
                process.end();
                return;
            }

            process.write("正在启动 JFR 分析服务...\n");

            String jarPath = resolveJarPath();
            if (jarPath == null) {
                process.write("✗ 未找到 arthas-jfr-backend-backend 的 jar 包，请先执行：\n");
                process.write("    mvn clean install\n");
                process.write("并确保 jar 包位于 arthas-jfr-backend/target/ 目录下\n");
                process.end();
                return;
            }
            ProcessBuilder builder = new ProcessBuilder(
                    "java", "-jar", jarPath
            );
            builder.inheritIO();
            builder.start();

            isStarted = true;
            process.write("✓ 服务启动成功！访问 http://localhost:8200\n");
        } catch (Exception e) {
            process.write("✗ 启动失败: " + e.getMessage() + "\n");
            e.printStackTrace();
        }

        process.end();
    }

    private String resolveJarPath() {
        try {
            // 获取运行中的 jar 文件（arthas-core.jar）
            File runningJar = new File(JFRansCommand.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            // 获取项目根目录（arthas-master）：arthas-core.jar → arthas-bin → target → packaging → ..
            File packagingDir = runningJar.getParentFile().getParentFile().getParentFile(); // packaging/
            File projectRoot = packagingDir.getParentFile(); // arthas-master/

            // 查找 jfr-analyzer-backend/target/*.jar
            File backendTargetDir = new File(projectRoot, "arthas-jfr-backend/target");
            if (!backendTargetDir.exists()) {
                return null;
            }

            File[] jars = backendTargetDir.listFiles((dir, name) ->
                    name.startsWith("arthas-jfr-backend") && name.endsWith(".jar")
            );

            if (jars != null && jars.length > 0) {
                // 选最新的
                File latest = jars[0];
                for (File f : jars) {
                    if (f.lastModified() > latest.lastModified()) {
                        latest = f;
                    }
                }
                return latest.getAbsolutePath();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
