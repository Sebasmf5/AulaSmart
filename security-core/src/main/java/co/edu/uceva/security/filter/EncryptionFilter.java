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

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        System.out.println("[EncryptionFilter] === " + method + " " + path + " ===");

        // Rutas publicas: nunca cifrar/descifrar
        if (path.contains("/auth/") || path.contains("/crypto/")) {
            System.out.println("[EncryptionFilter] Public route, passing through");
            chain.doFilter(request, response);
            return;
        }

        String sessionId = httpRequest.getHeader("x-session-id");
        if (sessionId == null || sessionId.isEmpty()) {
            System.out.println("[EncryptionFilter] No x-session-id, passing through");
            chain.doFilter(request, response);
            return;
        }

        CryptoSession session = sessionStore.getSession(sessionId);
        if (session == null) {
            System.out.println("[EncryptionFilter] Session NOT found: " + sessionId);
            chain.doFilter(request, response);
            return;
        }
        System.out.println("[EncryptionFilter] Session found: " + sessionId);

        String upperMethod = method.toUpperCase();
        String contentType = httpRequest.getContentType();
        boolean isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
        boolean isFormUrlEncoded = contentType != null && contentType.toLowerCase().startsWith("application/x-www-form-urlencoded");

        HttpServletRequest requestToUse = httpRequest;

        // Solo descifrar body para POST/PUT/PATCH con contenido JSON
        if (!"GET".equals(upperMethod) && !"DELETE".equals(upperMethod) && !"OPTIONS".equals(upperMethod)
                && !"HEAD".equals(upperMethod) && !isMultipart && !isFormUrlEncoded) {

            String encryptedBody = readBodyAsString(httpRequest);
            System.out.println("[EncryptionFilter] Raw body (first 100 chars): " + encryptedBody.substring(0, Math.min(encryptedBody.length(), 100)));

            if (!encryptedBody.isBlank()) {
                try {
                    String payload = extractPayload(encryptedBody);
                    CryptoService crypto = new CryptoService(session.getAesKey());
                    String plaintext = crypto.decrypt(payload);
                    System.out.println("[EncryptionFilter] Decrypted: " + plaintext);

                    byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
                    requestToUse = new DecryptedRequestWrapper(httpRequest, plainBytes);
                } catch (Exception e) {
                    System.err.println("[EncryptionFilter] Decrypt FAILED: " + e.getMessage());
                    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\":\"Payload invalido: " + escapeJson(e.getMessage()) + "\"}");
                    return;
                }
            } else {
                System.out.println("[EncryptionFilter] Body is blank");
            }
        }

        // Capturar respuesta para cifrar
        CaptureResponseWrapper captureResponse = new CaptureResponseWrapper(httpResponse);
        chain.doFilter(requestToUse, captureResponse);

        int status = captureResponse.getStatus();
        byte[] responseBytes = captureResponse.getCapturedData();
        String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
        System.out.println("[EncryptionFilter] Response status: " + status + ", body (first 100 chars): " + responseBody.substring(0, Math.min(responseBody.length(), 100)));

        try {
            CryptoService crypto = new CryptoService(session.getAesKey());
            String encryptedResponse = crypto.encrypt(responseBody);
            String json = "{\"payload\":\"" + encryptedResponse + "\"}";
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
            httpResponse.getWriter().write(json);
            httpResponse.getWriter().flush();
            System.out.println("[EncryptionFilter] Response encrypted OK");
        } catch (Exception e) {
            System.err.println("[EncryptionFilter] Encrypt response FAILED: " + e.getMessage());
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Error cifrando respuesta\"}");
        }
    }

    /**
     * Lee el body del request como String SIN cerrar el InputStream original.
     * Usa lectura byte-a-byte para evitar cualquier cierre de streams.
     */
    private String readBodyAsString(HttpServletRequest request) throws IOException {
        InputStream is = request.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        // NO cerramos 'is'
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private String extractPayload(String body) {
        int start = body.indexOf("\"payload\":\"");
        if (start == -1) throw new RuntimeException("No payload field in: " + body.substring(0, Math.min(body.length(), 100)));
        start += "\"payload\":\"".length();
        int end = body.indexOf("\"", start);
        if (end == -1) throw new RuntimeException("No payload end");
        return body.substring(start, end);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Wrapper que reemplaza completamente el body del request.
     * IMPORTANTE: preserva el Content-Type original.
     */
    private static class DecryptedRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;
        private final int contentLength;

        public DecryptedRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
            this.contentLength = body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ByteArrayServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return contentLength;
        }

        @Override
        public long getContentLengthLong() {
            return contentLength;
        }

        @Override
        public String getHeader(String name) {
            // Preservar Content-Type original para que Spring MVC sepa que es JSON
            return super.getHeader(name);
        }
    }

    private static class ByteArrayServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream bais;
        private boolean finished = false;

        public ByteArrayServletInputStream(byte[] body) {
            this.bais = new ByteArrayInputStream(body);
        }

        @Override
        public int read() throws IOException {
            int b = bais.read();
            if (b == -1) {
                finished = true;
            }
            return b;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // synchronous
        }
    }

    private static class CaptureResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private PrintWriter writer;
        private boolean writerUsed = false;
        private int status = SC_OK;

        public CaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        public int getStatus() {
            return status;
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
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
            if (writerUsed && writer != null) {
                writer.flush();
            }
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
