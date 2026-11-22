package io.github.grupo6.gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Ventana fija por dirección del peer, con estado local acotado por instancia. */
@Component
@Order(-101)
public class LoginRateLimitFilter implements WebFilter {
    private static final byte[] MESSAGE = ("{\"code\":\"RATE_LIMITED\","
        + "\"message\":\"Demasiados intentos. Reintentar más tarde\"}").getBytes(StandardCharsets.UTF_8);
    private final int requests;
    private final int maxClients;
    private final long windowNanos;
    private final LongSupplier time;
    private final Map<String, Window> clients = new HashMap<>();

    @Autowired
    public LoginRateLimitFilter(@Value("${security.login-rate-limit.requests}") int requests,
            @Value("${security.login-rate-limit.window-seconds}") long windowSeconds,
            @Value("${security.login-rate-limit.max-clients}") int maxClients) {
        this(requests, windowSeconds, maxClients, System::nanoTime);
    }

    LoginRateLimitFilter(int requests, long windowSeconds, int maxClients, LongSupplier time) {
        if (requests < 1 || windowSeconds < 1 || maxClients < 1) {
            throw new IllegalArgumentException("Los límites de login deben ser positivos");
        }
        this.requests = requests;
        this.maxClients = maxClients;
        this.windowNanos = Duration.ofSeconds(windowSeconds).toNanos();
        this.time = time;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().elements().stream()
            .map(element -> element instanceof org.springframework.http.server.PathContainer.PathSegment segment
                ? segment.valueToMatch() : element.value())
            .collect(java.util.stream.Collectors.joining());
        if (request.getMethod() != HttpMethod.POST || !("/api/v1/auth/login".equals(path) || "/api/v1/auth/register".equals(path))) {
            return chain.filter(exchange);
        }
        var remote = request.getRemoteAddress();
        String client = remote != null && remote.getAddress() != null
            ? remote.getAddress().getHostAddress() : "unknown";
        long retry = acquire(client);
        if (retry == 0) {
            return chain.filter(exchange);
        }
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", Long.toString(retry));
        response.getHeaders().setCacheControl("no-store");
        return response.writeWith(Mono.just(response.bufferFactory().wrap(MESSAGE)));
    }

    private synchronized long acquire(String client) {
        long now = time.getAsLong();
        Window window = clients.get(client);
        if (window == null || now - window.start >= windowNanos) {
            if (window == null && clients.size() >= maxClients) {
                clients.entrySet().removeIf(entry -> now - entry.getValue().start >= windowNanos);
                if (clients.size() >= maxClients) {
                    return Math.ceilDiv(windowNanos, 1_000_000_000L);
                }
            }
            window = new Window(now);
            clients.put(client, window);
        }
        if (window.used >= requests) {
            return Math.max(1, Math.ceilDiv(windowNanos - (now - window.start), 1_000_000_000L));
        }
        window.used++;
        return 0;
    }

    private static final class Window {
        private final long start;
        private int used;
        private Window(long start) { this.start = start; }
    }
}
