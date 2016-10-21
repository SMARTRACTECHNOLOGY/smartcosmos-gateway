package net.smartcosmos.cluster.gateway.config;

import org.apache.commons.lang.CharEncoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
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

import net.smartcosmos.cluster.gateway.util.MessageService;
import net.smartcosmos.cluster.gateway.util.MessageServiceDefault;

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

    /**
     * Need to configure Undertow to allo non standard wrappers or the exception gets lost in the noice.  This way the
     * exception will bubble out to the custom error controller.
     *
     * @return a modified Undertow config factory
     */
    @Bean
    public UndertowEmbeddedServletContainerFactory embeddedServletContainerFactory() {

        UndertowEmbeddedServletContainerFactory factory = new UndertowEmbeddedServletContainerFactory();
        factory.addDeploymentInfoCustomizers((UndertowDeploymentInfoCustomizer) deploymentInfo -> deploymentInfo.setAllowNonStandardWrappers(true));
        return factory;
    }

    @Bean
    public MessageService messageService() {

        MessageServiceDefault messageService = new MessageServiceDefault();
        messageService.setBasenames("gateway-messages");
        messageService.setUseCodeAsDefaultMessage(true);
        messageService.setDefaultEncoding(CharEncoding.UTF_8);
        return messageService;
    }
}
