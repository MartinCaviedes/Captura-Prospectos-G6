package io.github.grupo6.gateway.error;

import java.net.ConnectException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/** Una conexión rechazada al destino debe comunicar indisponibilidad temporal. */
@Component
@Order(-2)
public class UpstreamErrorHandler implements WebExceptionHandler {
    private static final byte[] UNAVAILABLE = ("{\"code\":\"UPSTREAM_UNAVAILABLE\","
        + "\"message\":\"Servicio temporalmente no disponible\"}")
        .getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        Throwable cause = error;
        while (cause != null && !(cause instanceof ConnectException)) {
            cause = cause.getCause();
        }
        if (cause == null || exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(UNAVAILABLE)));
    }
}
