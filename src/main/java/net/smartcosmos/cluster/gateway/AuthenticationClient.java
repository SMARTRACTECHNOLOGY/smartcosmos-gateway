package net.smartcosmos.cluster.gateway;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import net.smartcosmos.cluster.gateway.domain.UserDetails;
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

    private final PasswordEncoder passwordEncoder;
    private final RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory;

    // read from the gateway config.  ultimately this should probably oull the auth-server config directly so there
    // is only one location for this information.
    private final SecurityResourceProperties securityResourceProperties;
    private String userDetailsServerLocationUri;
    private RestTemplate userDetailsRestTemplate;

    @Autowired
    public AuthenticationClient(
        RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory,
        SecurityResourceProperties securityResourceProperties,
        PasswordEncoder passwordEncoder) {
        this.ribbonClientHttpRequestFactory = ribbonClientHttpRequestFactory;
        this.securityResourceProperties = securityResourceProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        this.userDetailsServerLocationUri = securityResourceProperties.getUserDetails().getServer().getLocationUri();
        userDetailsRestTemplate = new RestTemplate(ribbonClientHttpRequestFactory);
    }

    public UserDetails readUser(UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken)
        throws InternalAuthenticationServiceException {
        try {
            return userDetailsRestTemplate.exchange(UriComponentsBuilder.fromHttpUrl(userDetailsServerLocationUri)
                                                        .pathSegment(ENDPOINT_USER_DETAILS_AUTHENTICATE)
                                                        .build().toString(),
                                                    HttpMethod.POST,
                                                    new HttpEntity<Object>(usernamePasswordAuthenticationToken),
                                                    UserDetails.class)
                .getBody();
        } catch (HttpClientErrorException e) {
            if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
                String msg = String.format("User Details Service not properly configured to use SMART COSMOS Security Credentials; all requests " +
                                           "will fail. cause: %s", e.toString());
                log.warn(msg);
                log.debug(msg, e);
                throw new InternalAuthenticationServiceException(msg, e);
            } else {
                String msg = String.format("Exception retrieving authorization user details for user: '%s'. cause: %s",
                                           usernamePasswordAuthenticationToken.getPrincipal(), e.toString());
                log.warn(msg);
                log.debug(msg, e);
                throw new InternalAuthenticationServiceException(msg, e);
            }
        } catch (Exception e) {
            String msg = String
                .format("Unknown exception authenticating '%s', cause: %s", usernamePasswordAuthenticationToken.getPrincipal(), e.toString());
            log.warn(msg);
            log.debug(msg, e);
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }
    }
}
