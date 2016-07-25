package net.smartcosmos.cluster.gateway.filters;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.security.oauth2.proxy.ProxyAuthenticationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import net.smartcosmos.cluster.gateway.AuthenticationClient;
import net.smartcosmos.cluster.gateway.domain.UserDetails;

/**
 * Filter that occurs before Zuul forwards the request to see if the provided request has a JWT.  If it does not, attempts to validate existing
 * authentication against the Auth Server and retrieve a JWT for the request.
 */
@Slf4j
@Service
public class PreAuthorizationFilter extends ZuulFilter {

    public static final String FILTER_TYPE_PRE = "pre";
    public static final String BASIC_AUTHENTICATION_TYPE = "Basic";
    private Map<String, ProxyAuthenticationProperties.Route> routes = new HashMap<>();
    private final AuthenticationClient authenticationClient;

    @Autowired
    public PreAuthorizationFilter(ProxyAuthenticationProperties properties, AuthenticationClient authenticationClient) {
        this.routes = properties.getRoutes();
        this.authenticationClient = authenticationClient;
    }

    @Override
    public String filterType() {
        return FILTER_TYPE_PRE;
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        if (StringUtils.startsWith(request.getHeader(HttpHeaders.AUTHORIZATION), BASIC_AUTHENTICATION_TYPE)) {
            log.debug("Attempting to authenticate user with BASIC authentication.");
            return true;
        }

        return false;
    }

    @Override
    public Object run() {
        UserDetails userDetails = authenticationClient.readUser(getAuthenticationCredentials());
        return null;
    }

    private UsernamePasswordAuthenticationToken getAuthenticationCredentials() {

        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        String base64Credentials = request.getHeader(HttpHeaders.AUTHORIZATION).substring(BASIC_AUTHENTICATION_TYPE.length()).trim();
        String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] values = decodedCredentials.split(":", 2);
        return new UsernamePasswordAuthenticationToken(values[0], values[1]);
    }
}
