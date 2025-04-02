package com.pragmatix.testcase;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
    public ByteArrayHttpMessageConverter() {
        super(new MediaType("application", "octet-stream"), MediaType.ALL);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return true;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    public byte[] readInternal(Class clazz, HttpInputMessage inputMessage) throws IOException {
        long contentLength = inputMessage.getHeaders().getContentLength();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(contentLength >= 0L ? (int) contentLength : 4096);
        StreamUtils.copy(inputMessage.getBody(), bos);
        return bos.toByteArray();
    }

    protected Long getContentLength(Object bytes, MediaType contentType) {
        return (long) ((byte[]) bytes).length;
    }

    protected void writeInternal(Object bytes, HttpOutputMessage outputMessage) throws IOException {
        StreamUtils.copy(((byte[]) bytes), outputMessage.getBody());
    }

}