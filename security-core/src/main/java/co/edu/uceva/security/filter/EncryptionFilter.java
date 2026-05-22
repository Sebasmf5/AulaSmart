package co.edu.uceva.security.filter;

import co.edu.uceva.security.crypto.CryptoService;
import co.edu.uceva.security.session.CryptoSession;
import co.edu.uceva.security.session.InMemorySessionStore;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class EncryptionFilter implements Filter {

    private final InMemorySessionStore sessionStore;

    public EncryptionFilter(InMemorySessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String sessionId = httpRequest.getHeader("x-session-id");
        if (sessionId == null || sessionId.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        CryptoSession session = sessionStore.getSession(sessionId);
        if (session == null) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Sesion criptografica invalida\"}");
            return;
        }

        // Solo leer body si no es GET/DELETE y tiene contenido
        String encryptedBody = "";
        if (!"GET".equalsIgnoreCase(httpRequest.getMethod())
                && !"DELETE".equalsIgnoreCase(httpRequest.getMethod())) {
            String contentLength = httpRequest.getHeader("Content-Length");
            if (contentLength != null && !contentLength.equals("0")) {
                encryptedBody = readBody(httpRequest);
            }
        }

        HttpServletRequest wrappedRequest = httpRequest;
        if (!encryptedBody.isEmpty()) {
            try {
                String payload = extractPayload(encryptedBody);
                CryptoService crypto = new CryptoService(session.getAesKey());
                String plaintext = crypto.decrypt(payload);
                wrappedRequest = new PlainTextRequestWrapper(httpRequest, plaintext.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Payload invalido\"}");
                return;
            }
        }

        CaptureResponseWrapper captureResponse = new CaptureResponseWrapper(httpResponse);
        chain.doFilter(wrappedRequest, captureResponse);

        // Leer response capturada, cifrar y enviar al cliente
        byte[] responseBytes = captureResponse.getCapturedData();
        String responseBody = new String(responseBytes, StandardCharsets.UTF_8);

        try {
            CryptoService crypto = new CryptoService(session.getAesKey());
            String encryptedResponse = crypto.encrypt(responseBody);
            String json = "{\"payload\":\"" + encryptedResponse + "\"}";

            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
            httpResponse.getWriter().write(json);
        } catch (Exception e) {
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Error cifrando respuesta\"}");
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String extractPayload(String body) {
        int start = body.indexOf("\"payload\":\"");
        if (start == -1) throw new RuntimeException("No payload field");
        start += "\"payload\":\"".length();
        int end = body.indexOf("\"", start);
        if (end == -1) throw new RuntimeException("No payload end");
        return body.substring(start, end);
    }

    private static class PlainTextRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        public PlainTextRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                private final ByteArrayInputStream bais = new ByteArrayInputStream(body);
                @Override public int read() { return bais.read(); }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }

    private static class CaptureResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final PrintWriter writer = new PrintWriter(outputStream);

        public CaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener listener) {}
                @Override public void write(int b) throws IOException { outputStream.write(b); }
            };
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return writer;
        }

        @Override
        public void flushBuffer() {
            // No hacer nada para evitar comitear el response original prematuramente
        }

        @Override
        public void setContentLength(int len) {
            // No delegar para evitar Content-Length incorrecto en el response original
        }

        @Override
        public void setContentLengthLong(long len) {
            // No delegar
        }

        public byte[] getCapturedData() {
            writer.flush();
            return outputStream.toByteArray();
        }
    }
}
