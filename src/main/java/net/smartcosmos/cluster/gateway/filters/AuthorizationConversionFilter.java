package net.smartcosmos.cluster.gateway.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.web.savedrequest.Enumerator;

import net.smartcosmos.cluster.gateway.AuthenticationClient;

/**
 * Filter that occurs before Zuul forwards the request to see if the provided request has a JWT.  If it does not, attempts to validate existing
 * authentication against the Auth Server and retrieve a JWT for the request.
 */
@Slf4j
public class AuthorizationConversionFilter implements Filter {

    public static final String FILTER_TYPE_PRE = "pre";
    public static final String BASIC_AUTHENTICATION_TYPE = "Basic";

    private final AuthenticationClient authenticationClient;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Autowired
    public AuthorizationConversionFilter(AuthenticationClient authenticationClient) {
        this.authenticationClient = authenticationClient;
    }

    private boolean isBasicAuthRequest(HttpServletRequest request) {
        return StringUtils.startsWith(request.getHeader(HttpHeaders.AUTHORIZATION), BASIC_AUTHENTICATION_TYPE);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (isBasicAuthRequest((HttpServletRequest) request)) {
            OAuth2AccessToken oauthToken = authenticationClient.getOauthToken(getAuthenticationCredentials((HttpServletRequest) request));
            TransmutedHeaderHttpServletRequest transmutedRequest = new TransmutedHeaderHttpServletRequest((HttpServletRequest) request);
            transmutedRequest.addHeader(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " " + oauthToken.getValue());
            chain.doFilter(transmutedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private UsernamePasswordAuthenticationToken getAuthenticationCredentials(HttpServletRequest request) {
        String base64Credentials = request.getHeader(HttpHeaders.AUTHORIZATION).substring(BASIC_AUTHENTICATION_TYPE.length()).trim();
        String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] values = decodedCredentials.split(":", 2);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(values[0], values[1]);
        return usernamePasswordAuthenticationToken;
    }

    /**
     * Allow us to re-write the Authorization header to provide the Bearer token.
     */
    public class TransmutedHeaderHttpServletRequest extends HttpServletRequestWrapper {
        private final Map<String, String> headers = new HashMap<>();

        public TransmutedHeaderHttpServletRequest(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String headerKey, String value) {
            headers.put(headerKey, value);
        }

        @Override
        public String getHeader(String headerKey) {
            if (headers.containsKey(headerKey)) {
                return headers.get(headerKey);
            }
            return super.getHeader(headerKey);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> headerNames = new HashSet<>(Collections.list(super.getHeaderNames()));
            headerNames.addAll(headers.keySet());

            return new Enumerator<>(headerNames);
        }

        @Override
        public int getIntHeader(String name) {
            return super.getIntHeader(name);
        }
    }
}
