package net.smartcosmos.cluster.gateway.config;

import java.security.KeyPair;

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
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.util.Assert;

import net.smartcosmos.security.SecurityResourceProperties;
import net.smartcosmos.security.user.SmartCosmosUserAuthenticationConverter;

/**
 * Configuration class for Gateway.
 */
@Configuration
@EnableGlobalAuthentication
@EnableConfigurationProperties({ SecurityResourceProperties.class })
@Profile("!test")
public class GatewayConfiguration extends GlobalAuthenticationConfigurerAdapter {

    @Autowired
    private SecurityResourceProperties securityResourceProperties;

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

        Assert.hasText(securityResourceProperties.getKeystore().getKeypair());
        Assert.notNull(securityResourceProperties.getKeystore().getLocation());

        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        ((DefaultAccessTokenConverter) converter.getAccessTokenConverter()).setUserTokenConverter(new SmartCosmosUserAuthenticationConverter());
        KeyPair keyPair = new KeyStoreKeyFactory(
            securityResourceProperties.getKeystore().getLocation(),
            securityResourceProperties.getKeystore().getPassword()).getKeyPair(
            securityResourceProperties.getKeystore().getKeypair(),
            securityResourceProperties.getKeystore()
                .getKeypairPassword());
        converter.setKeyPair(keyPair);
        return converter;
    }

}
