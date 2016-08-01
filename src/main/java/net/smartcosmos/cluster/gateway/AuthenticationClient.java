package net.smartcosmos.cluster.gateway;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.smartcosmos.security.SecurityResourceProperties;

/**
 *
 */
@Slf4j
@Service
@Profile("!test")
@EnableConfigurationProperties({ SecurityResourceProperties.class })
public class AuthenticationClient {

    public static final String ENDPOINT_USER_DETAILS_AUTHENTICATE = "authenticate";

    private final RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory;

    // read from the gateway config.  ultimately this should probably oull the auth-server config directly so there
    // is only one location for this information.
    private final SecurityResourceProperties securityResourceProperties;
    private String userDetailsServerLocationUri;
    private RestTemplate userDetailsRestTemplate;

    @Autowired
    public AuthenticationClient(
        RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory,
        SecurityResourceProperties securityResourceProperties) {
        this.ribbonClientHttpRequestFactory = ribbonClientHttpRequestFactory;
        this.securityResourceProperties = securityResourceProperties;
    }

    @PostConstruct
    public void init() {
        userDetailsServerLocationUri = securityResourceProperties.getUserDetails().getServer().getLocationUri();
        List<ClientHttpRequestInterceptor> interceptors = Collections.<ClientHttpRequestInterceptor>singletonList(
            new BasicAuthorizationInterceptor(securityResourceProperties.getUserDetails().getUser().getName(),
                                              securityResourceProperties.getUserDetails().getUser().getPassword()));
        userDetailsRestTemplate = new RestTemplate(new InterceptingClientHttpRequestFactory(ribbonClientHttpRequestFactory, interceptors));
        //        userDetailsRestTemplate = new RestTemplate(ribbonClientHttpRequestFactory);
    }

    public OAuth2AccessToken getOauthToken(UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken)
        throws InternalAuthenticationServiceException {
        try {
            //            OAuth2AccessToken oAuth2AccessToken = readAccesstoken(usernamePasswordAuthenticationToken);
            URI uri = UriComponentsBuilder.fromHttpUrl(userDetailsServerLocationUri)
                .pathSegment("oauth/token")
                .queryParam("grant_type", "password")
                // .queryParam("scope","read")
                .queryParam("username", usernamePasswordAuthenticationToken.getPrincipal().toString())
                .queryParam("password", usernamePasswordAuthenticationToken.getCredentials().toString())
                .build().toUri();
            return userDetailsRestTemplate.exchange(uri,
                                                    HttpMethod.POST,
                                                    null,
                                                    OAuth2AccessToken.class)
                .getBody();
        } catch (HttpClientErrorException e) {
            String msg;
            if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
                msg = String.format("User Details Service not properly configured to use SMART COSMOS Security Credentials; all requests " +
                                    "will fail. cause: %s", e.toString());
            } else {
                msg = String.format("Exception retrieving authorization user details for user: '%s'. cause: %s",
                                    usernamePasswordAuthenticationToken.getPrincipal(), e.toString());
            }
            log.error(msg, e);
            log.debug(msg, e);
            throw new InternalAuthenticationServiceException(msg, e);
        } catch (Exception e) {
            String msg = String.format("Unknown exception authenticating '%s', cause: %s",
                                       usernamePasswordAuthenticationToken.getPrincipal(), e.toString());
            log.error(msg, e);
            log.debug(msg, e);
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }
    }

    private static class BasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {
        private final String username;
        private final String password;

        BasicAuthorizationInterceptor(String username, String password) {
            this.username = username;
            this.password = (password == null ? "" : password);
        }

        @Override
        public ClientHttpResponse intercept(
            HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
            String token = Base64Utils.encodeToString((this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));
            request.getHeaders().add("Authorization", "Basic " + token);
            return execution.execute(request, body);
        }
    }

}
