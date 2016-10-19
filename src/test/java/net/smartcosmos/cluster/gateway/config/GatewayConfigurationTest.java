package net.smartcosmos.cluster.gateway.config;

import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;

/**
 *
 */
@Configuration
@EnableGlobalAuthentication
@EnableWebSecurity
@Profile("test")
public class GatewayConfigurationTest {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory(SpringClientFactory clientFactory) {
        return new RibbonClientHttpRequestFactory(clientFactory);
    }

    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        return new JwtAccessTokenConverter();
    }

    @Bean
    public RestTemplate authServerRestTemplate() {

        return mock(RestTemplate.class);
    }
}
