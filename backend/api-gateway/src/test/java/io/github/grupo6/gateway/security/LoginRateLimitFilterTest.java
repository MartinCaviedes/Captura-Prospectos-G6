package io.github.grupo6.gateway.security;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimitFilterTest {
    MockServerWebExchange call(LoginRateLimitFilter filter, String ip, String path, String forwarded) {
        var request = MockServerHttpRequest.method(org.springframework.http.HttpMethod.POST, java.net.URI.create(path)).remoteAddress(new InetSocketAddress(ip, 12345))
            .header("X-Forwarded-For", forwarded).build();
        var exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, next -> {
            next.getResponse().setStatusCode(HttpStatus.OK);
            return next.getResponse().setComplete();
        }).block();
        return exchange;
    }

    @Test void rejectsAtLimitAndRecoversAfterWindow() {
        var time = new AtomicLong();
        var filter = new LoginRateLimitFilter(2, 60, 10, time::get);
        assertEquals(HttpStatus.OK, call(filter,"127.0.0.1","/api/v1/auth/login","a").getResponse().getStatusCode());
        assertEquals(HttpStatus.OK, call(filter,"127.0.0.1","/api/v1/auth/login","b").getResponse().getStatusCode());
        var blocked = call(filter,"127.0.0.1","/api/v1/auth/login","c");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.getResponse().getStatusCode());
        assertEquals("60", blocked.getResponse().getHeaders().getFirst("Retry-After"));
        assertTrue(blocked.getResponse().getBodyAsString().block().contains("RATE_LIMITED"));
        time.set(60_000_000_000L);
        assertEquals(HttpStatus.OK, call(filter,"127.0.0.1","/api/v1/auth/login","d").getResponse().getStatusCode());
    }

    @Test void boundsClientMemoryAndReclaimsExpiredEntries() {
        var time = new AtomicLong();
        var filter = new LoginRateLimitFilter(2, 60, 1, time::get);
        assertEquals(HttpStatus.OK, call(filter,"127.0.0.1","/api/v1/auth/login","a").getResponse().getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, call(filter,"127.0.0.2","/api/v1/auth/login","b").getResponse().getStatusCode());
        time.set(60_000_000_000L);
        assertEquals(HttpStatus.OK, call(filter,"127.0.0.2","/api/v1/auth/login","c").getResponse().getStatusCode());
    }

    @Test void ignoresSpoofedForwardingHeadersAndRecognizesEncodedLoginPath() {
        var filter = new LoginRateLimitFilter(1,60,10,() -> 0L);
        call(filter,"127.0.0.1","/api/v1/auth/login","10.0.0.1");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
            call(filter,"127.0.0.1","/api/v1/auth/%6cogin","10.0.0.2").getResponse().getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
            call(filter,"127.0.0.1","/api/v1/auth/login;test=value","a").getResponse().getStatusCode());
        assertEquals(HttpStatus.OK,call(filter,"127.0.0.2","/api/v1/auth/login","a").getResponse().getStatusCode());
        assertEquals(HttpStatus.OK,call(filter,"127.0.0.1","/api/v1/auth/logout","a").getResponse().getStatusCode());
    }

    @Test void invalidConfigurationFailsAtStartup() {
        assertThrows(IllegalArgumentException.class, () -> new LoginRateLimitFilter(0,60,1,() -> 0L));
        assertThrows(IllegalArgumentException.class, () -> new LoginRateLimitFilter(1,0,1,() -> 0L));
        assertThrows(IllegalArgumentException.class, () -> new LoginRateLimitFilter(1,60,0,() -> 0L));
    }

    @Test void registrationSharesBoundedPublicAuthenticationQuota() {
        var filter = new LoginRateLimitFilter(1,60,10,() -> 0L);
        assertEquals(HttpStatus.OK,call(filter,"127.0.0.1","/api/v1/auth/register","a").getResponse().getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,call(filter,"127.0.0.1","/api/v1/auth/login","b").getResponse().getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,call(filter,"127.0.0.1","/api/v1/auth/%72egister;test=value","c").getResponse().getStatusCode());
    }
}
