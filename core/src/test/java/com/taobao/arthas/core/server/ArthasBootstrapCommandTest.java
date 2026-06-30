package com.taobao.arthas.core.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.config.BinderUtils;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.env.ArthasEnvironment;
import com.taobao.arthas.core.env.PropertiesPropertySource;
import com.taobao.arthas.core.server.testsupport.ExternalTestCommand;
import com.taobao.arthas.core.server.testsupport.ExternalTestCommandResolver;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.CommandRegistry;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.NoOpHandler;

public class ArthasBootstrapCommandTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArthasBootstrapCommandTest.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testBindCommandLocations() {
        Properties properties = new Properties();
        properties.put("arthas.commandLocations", "/tmp/ext-command.jar,/tmp/ext-commands");

        ArthasEnvironment arthasEnvironment = new ArthasEnvironment();
        arthasEnvironment.addLast(new PropertiesPropertySource("test", properties));

        Configure configure = new Configure();
        BinderUtils.inject(arthasEnvironment, configure);

        assertThat(configure.getCommandLocations()).isEqualTo("/tmp/ext-command.jar,/tmp/ext-commands");
    }

    @Test
    public void testResolveCommandLocationUrls() throws Exception {
        Path tempDir = temporaryFolder.newFolder("arthas-command-locations").toPath();
        Path firstJar = Files.createFile(tempDir.resolve("b.jar"));
        Path secondJar = Files.createFile(tempDir.resolve("a.jar"));
        Files.createFile(tempDir.resolve("c.txt"));

        List<URL> urls = ArthasBootstrap.resolveCommandLocationUrls(firstJar + "," + tempDir, null, LOGGER);

        assertThat(urls).hasSize(2);
        assertThat(urls.stream().map(url -> {
            try {
                return new File(url.toURI()).getName();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()))
                        .containsExactly("b.jar", "a.jar");
    }

    @Test
    public void testResolveCommandLocationUrlsLoadsDefaultCommandsDirectory() throws Exception {
        Path arthasHome = temporaryFolder.newFolder("arthas-home").toPath();
        Path commandsDirectory = Files.createDirectory(arthasHome.resolve("commands"));
        Files.createFile(commandsDirectory.resolve("b.jar"));
        Files.createFile(commandsDirectory.resolve("a.jar"));
        Path configuredJar = temporaryFolder.newFile("arthas-configured-command.jar").toPath();

        List<URL> urls = ArthasBootstrap.resolveCommandLocationUrls(configuredJar.toString(), arthasHome.toString(), LOGGER);

        assertThat(urls).hasSize(3);
        assertThat(urls.stream().map(url -> {
            try {
                return new File(url.toURI()).getName();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()))
                        .containsExactly(configuredJar.getFileName().toString(), "a.jar", "b.jar");
    }

    @Test
    public void testResolveCommandLocationUrlsAcceptsUppercaseJarExtension() throws Exception {
        Path tempDir = temporaryFolder.newFolder("arthas-command-locations").toPath();
        Files.createFile(tempDir.resolve("external-command.JAR"));
        Files.createFile(tempDir.resolve("external-command.txt"));

        List<URL> urls = ArthasBootstrap.resolveCommandLocationUrls(tempDir.toString(), null, LOGGER);

        assertThat(urls).hasSize(1);
        assertThat(new File(urls.get(0).toURI()).getName()).isEqualTo("external-command.JAR");
    }

    @Test
    public void testAppendCommandUrlsAndLoadExternalCommandResolvers() throws Throwable {
        Path externalJar = createExternalResolverJar();
        TestCommandClassLoader classLoader = new TestCommandClassLoader();

        assertThat(ArthasBootstrap.loadExternalCommandResolvers(classLoader, LOGGER)).isEmpty();

        ArthasBootstrap.appendCommandUrls(classLoader, Collections.singletonList(externalJar.toUri().toURL()));

        List<CommandResolver> resolvers = ArthasBootstrap.loadExternalCommandResolvers(classLoader, LOGGER);
        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0).commands()).extracting(Command::name).containsExactly("external-test");
    }

    @Test
    public void testLoadExternalCommandResolversSkipsBrokenProvider() throws Throwable {
        Path externalJar = createExternalResolverJar("com.example.MissingCommandResolver\n"
                        + ExternalTestCommandResolver.class.getName() + "\n");
        TestCommandClassLoader classLoader = new TestCommandClassLoader();

        ArthasBootstrap.appendCommandUrls(classLoader, Collections.singletonList(externalJar.toUri().toURL()));

        List<CommandResolver> resolvers = ArthasBootstrap.loadExternalCommandResolvers(classLoader, LOGGER);
        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0).commands()).extracting(Command::name).containsExactly("external-test");
    }

    @Test
    public void testCreateExternalCommandRegistrySkipsReservedAndDuplicateCommands() {
        List<CommandResolver> reservedResolvers = Collections.singletonList(new StaticCommandResolver("reserved"));
        List<CommandResolver> externalResolvers = Arrays.asList(new StaticCommandResolver("external-a", "reserved"),
                        new StaticCommandResolver("external-a", "external-b"));

        CommandRegistry registry = ArthasBootstrap.createExternalCommandRegistry(reservedResolvers, externalResolvers, LOGGER);

        assertThat(registry).isNotNull();
        Set<String> commandNames = registry.commands().stream().map(Command::name).collect(Collectors.toSet());
        assertThat(commandNames).containsExactlyInAnyOrder("external-a", "external-b");
    }

    private Path createExternalResolverJar() throws IOException {
        return createExternalResolverJar(ExternalTestCommandResolver.class.getName() + "\n");
    }

    private Path createExternalResolverJar(String serviceContent) throws IOException {
        Path jarFile = temporaryFolder.newFile("arthas-external-command.jar").toPath();
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile))) {
            writeClassEntry(jarOutputStream, ExternalTestCommand.class);
            writeClassEntry(jarOutputStream, ExternalTestCommandResolver.class);
            writeTextEntry(jarOutputStream, "META-INF/services/com.taobao.arthas.core.shell.command.CommandResolver",
                            serviceContent);
        }
        return jarFile;
    }

    private void writeClassEntry(JarOutputStream jarOutputStream, Class<?> type) throws IOException {
        String resource = type.getName().replace('.', '/') + ".class";
        try (InputStream inputStream = type.getClassLoader().getResourceAsStream(resource)) {
            assertThat(inputStream).isNotNull();
            writeEntry(jarOutputStream, resource, toByteArray(inputStream));
        }
    }

    private void writeTextEntry(JarOutputStream jarOutputStream, String name, String content) throws IOException {
        writeEntry(jarOutputStream, name, content.getBytes(StandardCharsets.UTF_8));
    }

    private void writeEntry(JarOutputStream jarOutputStream, String name, byte[] bytes) throws IOException {
        JarEntry jarEntry = new JarEntry(name);
        jarOutputStream.putNextEntry(jarEntry);
        jarOutputStream.write(bytes);
        jarOutputStream.closeEntry();
    }

    private byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static class TestCommandClassLoader extends URLClassLoader {
        TestCommandClassLoader() {
            super(new URL[0], ArthasBootstrapCommandTest.class.getClassLoader());
        }

        public void appendURL(URL url) {
            super.addURL(url);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            }

            if (name.startsWith("java.") || name.startsWith("sun.")) {
                return super.loadClass(name, resolve);
            }

            try {
                Class<?> foundClass = findClass(name);
                if (resolve) {
                    resolveClass(foundClass);
                }
                return foundClass;
            } catch (ClassNotFoundException e) {
                return super.loadClass(name, resolve);
            }
        }
    }

    private static class StaticCommandResolver implements CommandResolver {
        private final List<Command> commands;

        StaticCommandResolver(String... commandNames) {
            this.commands = Arrays.stream(commandNames).map(StaticCommand::new).collect(Collectors.toList());
        }

        @Override
        public List<Command> commands() {
            return commands;
        }
    }

    private static class StaticCommand extends Command {
        private final String name;
        private final Handler<CommandProcess> handler = new NoOpHandler<CommandProcess>();

        StaticCommand(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Handler<CommandProcess> processHandler() {
            return handler;
        }
    }
}
