package net.smartcosmos.cluster.gateway;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * Definition for a class to contact the auth server.
 */
public interface AuthenticationClient {
    String PATH_OAUTH_TOKEN_REQUEST = "oauth/token";
    String PARAM_GRANT_TYPE = "grant_type";
    String PARAM_USERNAME = "username";
    String PARAM_PASSWORD = "password";
    String GRANT_TYPE_PASSWORD = "password";
    /**
     * Get the OAuth2 Token for the user with the provided username and password.
     *
     * @param username the username provided
     * @param password the password provided
     * @return the OAUth2 JWT based token
     */
    OAuth2AccessToken getOauthToken(String username, String password);
}
