package net.smartcosmos.cluster.gateway.util;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * A utility class that retrieves messages from PropertyResourceBundles.
 */
public class MessageServiceDefault extends ResourceBundleMessageSource implements MessageService {

    @Override
    public String getMessage(String code) {

        return getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage(String prefix, String code) {

        return getMessage(prefix + "." + code, null, LocaleContextHolder.getLocale());
    }

    /**
     * Get the specified message with the default Locale.
     *
     * @param code the message code to retrieve
     * @param args the arguments to replace
     * @param defaultMessage the default message to use is the messge is not found
     * @return the message string with parameters replaced
     */
    public String getMessage(String code, Object[] args, String defaultMessage) {

        return getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Get the specified message with the default Locale.
     *
     * @param code the message code to retrieve
     * @param args the arguments to replace
     * @return the message string with parameters replaced
     */
    public String getMessage(String code, Object[] args) {

        return getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * Get the specified message with the default Locale.
     *
     * @param prefix the prefix value of a message code
     * @param code the message code to retrieve
     * @param args the arguments to replace
     * @param defaultMessage the default message to use is the messge is not found
     * @return the message string with parameters replaced
     */
    public String getMessage(String prefix, String code, Object[] args, String defaultMessage) {

        return getMessage(prefix + "." + code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Get the specified message with the default Locale.
     *
     * @param prefix the prefix value of a message code
     * @param code the message code to retrieve
     * @param args the arguments to replace
     * @return the message string with parameters replaced
     */
    public String getMessage(String prefix, String code, Object[] args) {

        return getMessage(prefix + "." + code, args, LocaleContextHolder.getLocale());
    }
}
