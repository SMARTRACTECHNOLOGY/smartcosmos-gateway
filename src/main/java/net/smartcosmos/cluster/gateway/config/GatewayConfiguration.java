package net.smartcosmos.cluster.gateway.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import net.smartcosmos.cluster.gateway.rest.AuthenticationErrorHandler;

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
     * Need to configure Undertow to allow non standard wrappers or the exception gets lost in the noice.  This way the
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
    @Autowired
    public RestTemplate authServerRestTemplate(
        RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory,
        AuthenticationErrorHandler authenticationErrorHandler,
        AuthenticationServerConnectionProperties authServerConnectionProperties) {

        List<ClientHttpRequestInterceptor> interceptors = Collections.<ClientHttpRequestInterceptor>singletonList(
            new BasicAuthorizationInterceptor(authServerConnectionProperties.getName(),
                                              authServerConnectionProperties.getPassword()));
        RestTemplate restTemplate = new RestTemplate(new InterceptingClientHttpRequestFactory(ribbonClientHttpRequestFactory, interceptors));
        restTemplate.setErrorHandler(authenticationErrorHandler);

        return restTemplate;
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
            request.getHeaders()
                .add(HttpHeaders.AUTHORIZATION, BASIC_AUTHENTICATION_HEADER + token);
            return execution.execute(request, body);
        }
    }
}
