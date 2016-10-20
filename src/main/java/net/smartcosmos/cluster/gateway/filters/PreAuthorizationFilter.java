package net.smartcosmos.cluster.gateway.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.ExecutionStatus;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.ZuulFilterResult;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UrlPathHelper;

import net.smartcosmos.cluster.gateway.AuthenticationClient;
import net.smartcosmos.cluster.gateway.domain.ErrorResponse;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

/**
 * <p>Filter that occurs before Zuul forwards the request to see if the provided request has a JWT.  If it does not, attempts to validate existing
 * authentication against the Auth Server and retrieve a JWT for the request.</p>
 * <p>The filter is only applied if the route for the request is not configured to be handled locally (forwarding request), i.e. it doesn't match a
 * route like the following:</p>
 * <pre>
 *     forward-route:
 *       path: /local/**
 *       url: forward:/**
 * </pre>
 */
@Slf4j
@Service
public class PreAuthorizationFilter extends ZuulFilter {

    private static final String FILTER_TYPE_PRE = "pre";
    private static final Integer FILTER_ORDER = 2;

    private static final String BASIC_AUTHENTICATION_TYPE = "Basic";
    private static final String REQUEST_PATH_OAUTH = "oauth";
    private static final String LOCAL_HANDLING_PREFIX = "forward:";

    private final AuthenticationClient authenticationClient;
    private final RouteLocator routeLocator;
    private final UrlPathHelper urlPathHelper;

    @Autowired
    public PreAuthorizationFilter(AuthenticationClient authenticationClient, RouteLocator routeLocator) {

        this.authenticationClient = authenticationClient;
        this.routeLocator = routeLocator;

        urlPathHelper = new UrlPathHelper();
    }

    @Override
    public String filterType() {

        return FILTER_TYPE_PRE;
    }

    @Override
    public int filterOrder() {

        return FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {

        return !isForwardingRoute() && !isAuthorizationPath() && isBasicAuthRequest();
    }

    public boolean isForwardingRoute() {

        final String requestUri = urlPathHelper.getPathWithinApplication(getRequest());

        Route route = routeLocator.getMatchingRoute(requestUri);
        if (route != null && isNotBlank(route.getLocation())) {
            return route.getLocation()
                .startsWith(LOCAL_HANDLING_PREFIX);
        }
        return false;
    }

    public boolean isBasicAuthRequest() {

        return StringUtils.startsWith(HTTPRequestUtils.getInstance()
                                          .getHeaderValue(HttpHeaders.AUTHORIZATION), BASIC_AUTHENTICATION_TYPE);
    }

    public boolean isAuthorizationPath() {

        String path = getRequest().getRequestURI();
        return StringUtils.startsWith(path, REQUEST_PATH_OAUTH) || StringUtils.startsWith(path, "/" + REQUEST_PATH_OAUTH);
    }

    protected HttpServletRequest getRequest() {

        RequestContext ctx = RequestContext.getCurrentContext();
        return ctx.getRequest();
    }

    @Override
    public Object run() {

        String[] authCredentials = null;
        try {
            authCredentials = getAuthenticationCredentials();
            OAuth2AccessToken oauthToken = authenticationClient.getOauthToken(authCredentials[0], authCredentials[1]);
            RequestContext ctx = RequestContext.getCurrentContext();
            ctx.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " " + oauthToken.getValue());
        } catch (BadCredentialsException e) {
            log.warn("Authentication request failed. User: '{}', Cause: '{}'", authCredentials[0], e.getMessage());
            setErrorResponse(UNAUTHORIZED, "Access Denied");
        } catch (Throwable throwable) {
            log.warn("Exception processing authentication request. user: '{}', cause: '{}'",
                     // if we have Basic Auth credentials, return only the username
                     authCredentials != null && authCredentials.length == 2 ? authCredentials[0] : ArrayUtils.toString(authCredentials),
                     throwable.toString());
            return new ZuulFilterResult(ExecutionStatus.FAILED);
        }
        return new ZuulFilterResult(ExecutionStatus.SUCCESS);
    }

    protected String[] getAuthenticationCredentials() {

        HttpServletRequest request = getRequest();

        String base64Credentials = request.getHeader(HttpHeaders.AUTHORIZATION)
            .substring(BASIC_AUTHENTICATION_TYPE.length())
            .trim();
        String decodedCredentials = new String(Base64.getDecoder()
                                                   .decode(base64Credentials), StandardCharsets.UTF_8);
        return decodedCredentials.split(":", 2);
    }

    protected void setErrorResponse(HttpStatus statusCode, String message) {

        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setResponseStatusCode(statusCode.value());
        ctx.addZuulResponseHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE);
        if (ctx.getResponseBody() == null) {
            ctx.setResponseBody(getResponseBody(statusCode, message, getRequest().getServletPath()));
            ctx.setSendZuulResponse(false);
        }
    }

    protected String getResponseBody(HttpStatus statusCode, String message, String path) {

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ErrorResponse responseBody = ErrorResponse.builder()
                .timestamp(System.currentTimeMillis())
                .status(statusCode.value())
                .error(statusCode.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
            return objectMapper.writeValueAsString(responseBody);
        } catch (IOException e) {
            return String.format("{\"message\": \"%s\"}", message);
        }
    }
}
