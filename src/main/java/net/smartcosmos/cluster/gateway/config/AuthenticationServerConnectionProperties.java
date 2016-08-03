package net.smartcosmos.cluster.gateway.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties read from the configuration files to connect to the authentication server.
 */
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ConfigurationProperties("smartcosmos.security.resource.authorization-server")
public class AuthenticationServerConnectionProperties {
    private String locationUri = "http://smartcosmos-auth-server";
    private String name = "smartcosmosclient";
    private String password = "LkRv4Z-=caBcx.zX";
}
