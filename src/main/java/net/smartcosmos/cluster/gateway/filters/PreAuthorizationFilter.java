package net.smartcosmos.cluster.gateway.filters;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.security.oauth2.proxy.ProxyAuthenticationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import net.smartcosmos.cluster.gateway.AuthenticationClient;

/**
 * Filter that occurs before Zuul forwards the request to see if the provided request has a JWT.  If it does not, attempts to validate existing
 * authentication against the Auth Server and retrieve a JWT for the request.
 */
@Slf4j
@Service
public class PreAuthorizationFilter extends ZuulFilter {

    public static final String FILTER_TYPE_PRE = "pre";
    public static final String BASIC_AUTHENTICATION_TYPE = "Basic";

    //    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
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
        return isBasicAuthRequest();
    }

    private boolean isBasicAuthRequest() {
        return StringUtils.startsWith(HTTPRequestUtils.getInstance().getHeaderValue(HttpHeaders.AUTHORIZATION), BASIC_AUTHENTICATION_TYPE);
    }

    private HttpServletRequest getRequest() {
        RequestContext ctx = RequestContext.getCurrentContext();
        return ctx.getRequest();
    }

    @Override
    public Object run() {
        String[] authCredentials = getAuthenticationCredentials();
        OAuth2AccessToken oauthToken = authenticationClient.getOauthToken(authCredentials[0], authCredentials[1]);
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " " + oauthToken.getValue());
        return null;
    }

    private String[] getAuthenticationCredentials() {

        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        String base64Credentials = request.getHeader(HttpHeaders.AUTHORIZATION).substring(BASIC_AUTHENTICATION_TYPE.length()).trim();
        String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        return decodedCredentials.split(":", 2);
    }

}
