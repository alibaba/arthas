package com.alibaba.arthas.tunnel.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SimpleHttpResponseTest {

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        SimpleHttpResponse response = new SimpleHttpResponse();
        response.setStatus(200);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "text/plain");
        response.setHeaders(headers);

        String content = "Hello, world!";
        response.setContent(content.getBytes());

        byte[] bytes = SimpleHttpResponse.toBytes(response);

        SimpleHttpResponse deserializedResponse = SimpleHttpResponse.fromBytes(bytes);

        assertEquals(response.getStatus(), deserializedResponse.getStatus());
        assertEquals(response.getHeaders(), deserializedResponse.getHeaders());
        assertArrayEquals(response.getContent(), deserializedResponse.getContent());
    }

    private static byte[] toBytes(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            out.flush();
            return bos.toByteArray();
        }
    }

    @Test(expected = InvalidClassException.class)
    public void testDeserializationWithUnauthorizedClass() throws IOException, ClassNotFoundException {
        Date date = new Date();

        byte[] bytes = toBytes(date);

        // Try to deserialize the object with an unauthorized class
        // This should throw an InvalidClassException
        SimpleHttpResponse.fromBytes(bytes);
    }

}
