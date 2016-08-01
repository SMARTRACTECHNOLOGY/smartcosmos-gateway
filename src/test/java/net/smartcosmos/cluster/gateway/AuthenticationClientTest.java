package net.smartcosmos.cluster.gateway;

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

/**
 * Test instance.
 */
@Profile("test")
@Service
public class AuthenticationClientTest implements AuthenticationClient {
    @Override
    public OAuth2AccessToken getOauthToken(String username, String password) {
        return null;
    }
}
