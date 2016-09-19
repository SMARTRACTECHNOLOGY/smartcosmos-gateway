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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.smartcosmos.cluster.gateway.config.AuthenticationServerConnectionProperties;

/**
 * Connect to the Authentication service and get an OAuthToken for the basic auth credentials provided.
 */
@Slf4j
@Service
@EnableConfigurationProperties({ AuthenticationServerConnectionProperties.class })
public class AuthenticationClientDefault implements AuthenticationClient {

    private final RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory;
    private final AuthenticationServerConnectionProperties authServerConnectionProperties;
    private RestTemplate authServerRestTemplate;

    @Autowired
    public AuthenticationClientDefault(
        RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory,
        AuthenticationServerConnectionProperties authServerConnectionProperties) {
        this.ribbonClientHttpRequestFactory = ribbonClientHttpRequestFactory;
        this.authServerConnectionProperties = authServerConnectionProperties;
    }

    @PostConstruct
    public void init() {
        List<ClientHttpRequestInterceptor> interceptors = Collections.<ClientHttpRequestInterceptor>singletonList(
            new BasicAuthorizationInterceptor(authServerConnectionProperties.getName(),
                                              authServerConnectionProperties.getPassword()));
        authServerRestTemplate = new RestTemplate(new InterceptingClientHttpRequestFactory(ribbonClientHttpRequestFactory, interceptors));
    }

    @Override
    public OAuth2AccessToken getOauthToken(String username, String password)
        throws InternalAuthenticationServiceException {
            URI uri = UriComponentsBuilder.fromHttpUrl(authServerConnectionProperties.getLocationUri())
                .pathSegment(PATH_OAUTH_TOKEN_REQUEST)
                .queryParam(PARAM_GRANT_TYPE, GRANT_TYPE_PASSWORD)
                .queryParam(PARAM_USERNAME, username)
                .queryParam(PARAM_PASSWORD, password)
                .build().toUri();
            log.debug("Connecting to {} using username: {} to authenticate user.", uri, username);
            return authServerRestTemplate.exchange(uri,
                                                   HttpMethod.POST,
                                                   null,
                                                   OAuth2AccessToken.class)
                .getBody();
    }

    private static class BasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {
        private static final String BASIC_AUTHENTICATION_HEADER = "Basic ";
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
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, BASIC_AUTHENTICATION_HEADER + token);
            return execution.execute(request, body);
        }
    }

}
