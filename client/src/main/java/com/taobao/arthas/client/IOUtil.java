package com.taobao.arthas.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;

/***
 * This is a utility class providing a reader/writer capability required by the
 * weatherTelnet, rexec, rshell, and rlogin example programs. The only point of
 * the class is to hold the static method readWrite which spawns a reader thread
 * and a writer thread. The reader thread reads from a local input source
 * (presumably stdin) and writes the data to a remote output destination. The
 * writer thread reads from a remote input source and writes to a local output
 * destination. The threads terminate when the remote input source closes.
 ***/

public final class IOUtil {

    public static final void readWrite(final InputStream remoteInput, final OutputStream remoteOutput,
                    final InputStream localInput, final Writer localOutput) {
        Thread reader, writer;

        reader = new Thread() {
            @Override
            public void run() {
                int ch;

                try {
                    while (!interrupted() && (ch = localInput.read()) != -1) {
                        remoteOutput.write(ch);
                        remoteOutput.flush();
                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                }
            }
        };

        writer = new Thread() {
            @Override
            public void run() {
                try {
                    InputStreamReader reader = new InputStreamReader(remoteInput);
                    while (true) {
                        int singleChar = reader.read();
                        if (singleChar == -1) {
                            break;
                        }
                        localOutput.write(singleChar);
                        localOutput.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        writer.setPriority(Thread.currentThread().getPriority() + 1);

        writer.start();
        reader.setDaemon(true);
        reader.start();

        try {
            writer.join();
            reader.interrupt();
        } catch (InterruptedException e) {
            // Ignored
        }
    }

}