package net.smartcosmos.cluster.gateway.filters;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.security.oauth2.proxy.ProxyAuthenticationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Filter that occurs before Zuul forwards the request to see if the provided request has a JWT.  If it does not, attempts to validate existing
 * authentication against the Auth Server and retrieve a JWT for the request.
 */
@Slf4j
@Service
public class PreAuthorizationFilter extends ZuulFilter {

    private Map<String, ProxyAuthenticationProperties.Route> routes = new HashMap<>();

    @Autowired
    public PreAuthorizationFilter(ProxyAuthenticationProperties properties) {
        this.routes = properties.getRoutes();
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        log.info("shouldFilter");

        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (StringUtils.isEmpty(request.getHeader("Authentication"))) {
            return false;
        } else {
            log.info(request.getAuthType());

            return true;
        }

    }

    @Override
    public Object run() {
        return null;
    }
}
