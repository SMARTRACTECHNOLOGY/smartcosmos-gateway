package net.smartcosmos.cluster.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * @author voor
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = GatewayApplication.class)
@WebAppConfiguration
@ActiveProfiles("test")
@IntegrationTest({ "spring.cloud.config.enabled=false", "eureka.client.enabled:false" })
public class GatewayApplicationTest {

	@Test
	public void contextLoads() {

	}
}
