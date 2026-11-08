package senac.tsi.apipapas.idempotencia;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Lê o corpo da requisição uma vez (para calcular o hash) e permite que o controller o leia de novo. */
class CorpoEmCacheRequest extends HttpServletRequestWrapper {

    private final byte[] corpo;

    CorpoEmCacheRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.corpo = request.getInputStream().readAllBytes();
    }

    byte[] getCorpo() {
        return corpo;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream dados = new ByteArrayInputStream(corpo);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return dados.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return dados.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return dados.read(b, off, len);
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        String encoding = getCharacterEncoding();
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
