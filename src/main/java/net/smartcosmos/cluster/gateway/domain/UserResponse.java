package net.smartcosmos.cluster.gateway.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * This is the response from the User Details Service that will contain the necessary
 * information for caching purposes. While not required, if the password hash is filled
 * this will speed up authentication considerably, since it can be queried against the
 * native Spring Security Cache.
 * <p>
 * Unabashedly stolen from smartcosmos-auth-server.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "passwordHash")
public class UserResponse {

    private String tenantUrn;

    private String userUrn;

    private String username;

    private String passwordHash;

    private List<String> authorities;
}
