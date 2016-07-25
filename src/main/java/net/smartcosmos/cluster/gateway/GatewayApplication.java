package net.smartcosmos.cluster.gateway;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import net.smartcosmos.cluster.gateway.config.GatewayConfiguration;

/**
 * The Gateway provides the primary and preferred entryway into the SMART COSMOS Cluster.
 * This enables a simplistic way to guarantee load balancing and service discovery through
 * the documented API.
 *
 * @author voor
 */
@SpringBootApplication
@EnableOAuth2Sso
@EnableZuulProxy
@Import(GatewayConfiguration.class)
public class GatewayApplication extends WebSecurityConfigurerAdapter {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GatewayApplication.class).run(args);
    }

    @Override
    public void init(WebSecurity web) throws Exception {
        web.ignoring().anyRequest();
    }
}
