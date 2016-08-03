package net.smartcosmos.cluster.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for Gateway.
 */
@Configuration
@EnableGlobalAuthentication
@EnableConfigurationProperties({ AuthenticationServerConnectionProperties.class })
@Profile("!test")
public class GatewayConfiguration extends GlobalAuthenticationConfigurerAdapter {

    @Autowired
    private AuthenticationServerConnectionProperties securityResourceProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory(SpringClientFactory clientFactory) {
        return new RibbonClientHttpRequestFactory(clientFactory);
    }
}
