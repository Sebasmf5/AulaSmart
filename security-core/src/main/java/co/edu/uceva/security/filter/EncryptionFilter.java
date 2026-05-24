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

        String method = httpRequest.getMethod().toUpperCase();

        // Leer body solo si la peticion tiene contenido
        HttpServletRequest wrappedRequest = httpRequest;
        if (!"GET".equals(method) && !"DELETE".equals(method) && !"OPTIONS".equals(method)
                && !"HEAD".equals(method)) {
            try {
                String encryptedBody = readBody(httpRequest);
                if (encryptedBody != null && !encryptedBody.isBlank()) {
                    String payload = extractPayload(encryptedBody);
                    CryptoService crypto = new CryptoService(session.getAesKey());
                    String plaintext = crypto.decrypt(payload);
                    wrappedRequest = new PlainTextRequestWrapper(httpRequest,
                            plaintext.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Payload invalido: " + e.getMessage() + "\"}");
                return;
            }
        }

        CaptureResponseWrapper captureResponse = new CaptureResponseWrapper(httpResponse);
        chain.doFilter(wrappedRequest, captureResponse);

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
            httpResponse.getWriter().flush();
        } catch (Exception e) {
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Error cifrando respuesta\"}");
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String extractPayload(String body) {
        int start = body.indexOf("\"payload\":\"");
        if (start == -1) throw new RuntimeException("No payload field in: " + body.substring(0, Math.min(body.length(), 100)));
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
        private PrintWriter writer;
        private boolean writerUsed = false;

        public CaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener listener) {}
                @Override public void write(int b) { outputStream.write(b); }
            };
        }

        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            }
            writerUsed = true;
            return writer;
        }

        @Override
        public void flushBuffer() {
            // No propagar flush al response original
        }

        @Override
        public void setContentLength(int len) {}

        @Override
        public void setContentLengthLong(long len) {}

        public byte[] getCapturedData() {
            if (writerUsed && writer != null) {
                writer.flush();
            }
            return outputStream.toByteArray();
        }
    }
}
