package net.smartcosmos.cluster.gateway;

import net.smartcosmos.spring.EnableSmartCosmosSecurity;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

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
@EnableSmartCosmosSecurity
public class GatewayApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GatewayApplication.class).run(args);
    }

}
