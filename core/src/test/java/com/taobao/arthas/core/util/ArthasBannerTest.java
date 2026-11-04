package com.taobao.arthas.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.taobao.arthas.core.shell.ShellServerOptions;

public class ArthasBannerTest {

    @Test
    public void shouldRenderWelcomeWithoutCheckingForUpdates() {
        final ProxySelector previous = ProxySelector.getDefault();
        final AtomicInteger requests = new AtomicInteger();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                if ("arthas.aliyun.com".equals(uri.getHost())) {
                    requests.incrementAndGet();
                    // Fail before DNS or network I/O, even when testing the old implementation.
                    throw new IllegalStateException("Unexpected version check while rendering the welcome message");
                }
                return previous == null ? Collections.singletonList(Proxy.NO_PROXY) : previous.select(uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, IOException failure) {
                if (previous != null) {
                    previous.connectFailed(uri, address, failure);
                }
            }
        });
        try {
            assertThat(new ShellServerOptions().getWelcomeMessage()).contains(ArthasBanner.version());
            assertThat(ArthasBanner.welcome()).contains(ArthasBanner.version());
            assertThat(ArthasBanner.welcome(Collections.singletonMap("test", "welcome"))).contains("welcome");
            assertThat(requests.get()).isZero();
        } finally {
            ProxySelector.setDefault(previous);
        }
    }

    @Test
    public void shouldCheckLatestVersionWithoutWaitingForResponse() throws Exception {
        final HttpURLConnection connection = mock(HttpURLConnection.class);
        final CountDownLatch requestStarted = new CountDownLatch(1);
        final CountDownLatch responseReady = new CountDownLatch(1);
        final InputStream response = spy(new ByteArrayInputStream("4.3.5\n".getBytes(StandardCharsets.UTF_8)));
        when(connection.getInputStream()).thenAnswer(invocation -> {
            requestStarted.countDown();
            if (!responseReady.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Test response was not released");
            }
            return response;
        });
        final URL url = urlFor(connection);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        Thread worker = null;
        try {
            worker = caller.submit(new Callable<Thread>() {
                @Override
                public Thread call() {
                    return ArthasBanner.checkLatestVersionAsync(url);
                }
            }).get(1, TimeUnit.SECONDS);

            assertThat(requestStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(worker.isAlive()).isTrue();
            assertThat(worker.isDaemon()).isTrue();
            assertThat(worker.getName()).isEqualTo("arthas-version-check");
            assertThat(worker.getContextClassLoader()).isSameAs(ArthasBanner.class.getClassLoader());

            responseReady.countDown();
            worker.join(5000);
            assertThat(worker.isAlive()).isFalse();
            verify(connection).setConnectTimeout(1000);
            verify(connection).setReadTimeout(1000);
            verify(response).close();
            verify(connection).disconnect();
        } finally {
            responseReady.countDown();
            caller.shutdownNow();
            if (worker != null) {
                worker.interrupt();
                worker.join(5000);
            }
        }
    }

    @Test
    public void shouldReleaseConnectionWhenVersionCheckFails() throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getInputStream()).thenThrow(new IOException("Version service unavailable"));

        Thread worker = ArthasBanner.checkLatestVersionAsync(urlFor(connection));
        worker.join(5000);

        assertThat(worker.isAlive()).isFalse();
        verify(connection).getInputStream();
        verify(connection).disconnect();
    }

    private static URL urlFor(final HttpURLConnection connection) throws Exception {
        return new URL(null, "https://version-check.test/latest_version", new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                return connection;
            }
        });
    }
}
