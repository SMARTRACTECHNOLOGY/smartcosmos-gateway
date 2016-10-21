package net.smartcosmos.cluster.gateway.config;

import org.apache.commons.lang.CharEncoding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.smartcosmos.cluster.gateway.util.MessageService;
import net.smartcosmos.cluster.gateway.util.MessageServiceDefault;

@Configuration
public class ErrorMessageTestConfiguration {

    @Bean
    public MessageService messageService() {

        MessageServiceDefault messageService = new MessageServiceDefault();
        messageService.setBasenames("gateway-test-messages");
        messageService.setUseCodeAsDefaultMessage(true);
        messageService.setDefaultEncoding(CharEncoding.UTF_8);
        return messageService;
    }

}
