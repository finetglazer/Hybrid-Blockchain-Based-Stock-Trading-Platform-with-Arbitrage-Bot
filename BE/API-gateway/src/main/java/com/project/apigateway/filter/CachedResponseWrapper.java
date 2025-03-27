//package com.project.apigateway.filter;
//
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
//
//import java.io.ByteArrayOutputStream;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//
//public class CachedResponseWrapper extends ServerHttpResponseDecorator {
//
//    private final ByteArrayOutputStream cachedOutputStream = new ByteArrayOutputStream();
//    private final PrintWriter writer = new PrintWriter(new OutputStreamWriter(cachedOutputStream));
//
//    public CachedResponseWrapper(ServerHttpResponse response) {
//        super(response);
//    }
////
////    @Override
////    public PrintWriter getWriter() {
////        return writer;
////    }
//
//    public String getCachedBody() {
//        writer.flush();
//        return cachedOutputStream.toString();
//    }
//}
