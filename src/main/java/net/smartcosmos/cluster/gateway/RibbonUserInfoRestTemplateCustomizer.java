package net.smartcosmos.cluster.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateCustomizer;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

/**
 * Created by voor on 5/11/16.
 */
@Component
public class RibbonUserInfoRestTemplateCustomizer implements
    UserInfoRestTemplateCustomizer {

    private final RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory;

    @Autowired
    public RibbonUserInfoRestTemplateCustomizer(RibbonClientHttpRequestFactory ribbonClientHttpRequestFactory) {
        this.ribbonClientHttpRequestFactory = ribbonClientHttpRequestFactory;
    }

    @Override public void customize(OAuth2RestTemplate template) {
        template.setRequestFactory(ribbonClientHttpRequestFactory);
    }
}
