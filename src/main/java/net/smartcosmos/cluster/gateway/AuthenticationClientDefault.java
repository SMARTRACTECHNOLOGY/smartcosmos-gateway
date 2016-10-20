package net.smartcosmos.cluster.gateway;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.smartcosmos.cluster.gateway.config.AuthenticationServerConnectionProperties;

import static net.smartcosmos.cluster.gateway.util.UrlEncodingUtil.encode;

/**
 * Connect to the Authentication service and get an OAuthToken for the basic auth credentials provided.
 */
@Slf4j
@Service
@EnableConfigurationProperties({ AuthenticationServerConnectionProperties.class })
public class AuthenticationClientDefault implements AuthenticationClient {

    private final AuthenticationServerConnectionProperties authServerConnectionProperties;
    private RestTemplate authServerRestTemplate;

    @Autowired
    public AuthenticationClientDefault(
        @Qualifier("authServerRestTemplate") RestTemplate authServerRestTemplate,
        AuthenticationServerConnectionProperties authServerConnectionProperties) {

        this.authServerRestTemplate = authServerRestTemplate;
        this.authServerConnectionProperties = authServerConnectionProperties;
    }

    @Override
    public OAuth2AccessToken getOauthToken(String username, String password) throws AuthenticationException {

        String authServerUri = authServerConnectionProperties.getLocationUri();
        String uriString = UriComponentsBuilder.fromHttpUrl(authServerUri)
            .pathSegment(PATH_OAUTH_TOKEN_REQUEST)
            .queryParam(PARAM_GRANT_TYPE, GRANT_TYPE_PASSWORD)
            .queryParam(PARAM_USERNAME, encode(username))
            .queryParam(PARAM_PASSWORD, encode(password))
            .build()
            .toUriString();
        log.debug("Connecting to {} using username: {} to authenticate user.", authServerUri, username);

        try {
            return authServerRestTemplate.postForObject(uriString, null, OAuth2AccessToken.class);
        } catch (RestClientException e) {
            String message = String.format("Authenticating user %s with request %s failed: %s",
                                           username,
                                           uriString.replace(PARAM_PASSWORD + "=" + password, PARAM_PASSWORD + "=[PROTECTED]"),
                                           e.toString());
            log.warn(message);
            log.debug(message, e);
            throw new InternalAuthenticationServiceException(message, e);
        }
    }
}
