package dv.service.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dv.service.gateway.config.exceptions.UnAuthenticateException;
import dv.service.gateway.dtos.AppResponse;
import dv.service.gateway.dtos.BasicUserInfo;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Order(-1)
public class JwtAuthenticationGatewayFilterFactory implements GlobalFilter {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String PHONE_NUMBER_HEADER = "X-PHONE-NUMBER-Phone";
    private static final String DISPLAY_NAME_HEADER = "X-User-Display-Name";
    private static final String EMAIL_VERIFIED_HEADER = "X-User-Email-Verified";
    private static final String EMAIL_HEADER = "X-User-Email";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);

    public JwtAuthenticationGatewayFilterFactory(WebClient webClient) {
        this.webClient = webClient;
    }

    private final WebClient webClient;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> EXCLUDED_URL_PATTERNS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/*/health-check",
            "/eureka/**"
    );

    private static final List<String> OPTIONAL_AUTH_URL_PATTERNS = List.of(
            "/api/*/*/public/**"
    );

    private static final List<String> PREDEFINED_URL_PATTERNS = List.of(
            "/api/users/v1/public/auth/validate-access-token"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestPath = request.getURI().getPath();

        if (PREDEFINED_URL_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath))) {
            return this.onError(exchange, UnAuthenticateException.MESSAGE);
        }

        if (EXCLUDED_URL_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath))) {
            log.info("Path EXCLUDED from authentication: {}", requestPath);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = null;
        boolean hasToken = (authHeader != null && authHeader.startsWith("Bearer "));

        if (hasToken) {
            token = authHeader.replace("Bearer ", "");
        }

        if (!hasToken) {
            if (OPTIONAL_AUTH_URL_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath))) {
                log.info("Path OPTIONAL AUTH: {}. Allowing unauthenticated access (no token).", requestPath);
                return chain.filter(exchange);
            } else {
                log.info("Path REQUIRES AUTH: {}. Missing token.", requestPath);
                return this.onError(exchange, UnAuthenticateException.MESSAGE);
            }
        }

        final String finalToken = token;
        return webClient
                .get()
                .uri("http://USER-SERVICE/api/users/v1/public/auth/validate-access-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + finalToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        Mono.error(new UnAuthenticateException())
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        Mono.error(new UnAuthenticateException())
                )
                .bodyToMono(new ParameterizedTypeReference<AppResponse<BasicUserInfo>>() {
                })
                .flatMap(response -> {
                    if (response.getMessage().equalsIgnoreCase("success")) {
                        var userInfo = response.getData();
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header(USER_ID_HEADER, userInfo.getUserId().toString())
                                .header(EMAIL_HEADER, userInfo.getEmail())
                                .header(PHONE_NUMBER_HEADER, userInfo.getPhoneNumber())
                                .header(DISPLAY_NAME_HEADER, userInfo.getDisplayName())
                                .header(EMAIL_VERIFIED_HEADER, userInfo.getEmailVerified().toString())
                                .header(USER_ROLES_HEADER, StringUtils.join(userInfo.getRoles(), ","))
                                .build();
                        log.info("Authenticated user via user-service: {} for path: {}", userInfo.getUserId(), requestPath);
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    } else {
                        log.info("Token invalid according to user-service for path: {}", requestPath);
                        return this.onError(exchange, "Unauthorized access: Token invalid");
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Error calling user-service for token validation: " + e.getMessage());
                    if (OPTIONAL_AUTH_URL_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath))) {
                        log.info("Path OPTIONAL AUTH with token validation error. Allowing unauthenticated access.");
                        return chain.filter(exchange);
                    } else {
                        return this.onError(exchange, "Unauthorized access: Token validation failed");
                    }
                });
    }

    @SneakyThrows
    private Mono<Void> onError(ServerWebExchange exchange, String err) {
        var objectMapper = new ObjectMapper();

        var res = AppResponse.error(err, HttpStatus.UNAUTHORIZED);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        String jsonResponse = objectMapper.writeValueAsString(res);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(jsonResponse.getBytes()))
        );
    }
}
