package net.smartcosmos.cluster.gateway.util;

import org.springframework.context.MessageSource;

/**
 * Retrieve messages from PropertyResourceBundles.
 */
public interface MessageService extends MessageSource {

    /**
     * Return the message for the specified code.
     *
     * @param code the message code to retrieve
     * @return the message string with parameters replaced
     */
    public String getMessage(String code);

    /**
     * Return the message for the prefix and code provided.
     *
     * @param prefix the prefix value of a message code
     * @param code the message code to retrieve
     * @return the message string with parameters replaced
     */
    public String getMessage(String prefix, String code);

    /**
     * Get the specified message with the default Locale.
     *
     * @param code the message code to retrieve
     * @param args the arguments to replace
     * @param defaultMessage the default message to use is the messge is not found
     * @return the message string with parameters replaced
     */
    public String getMessage(String code, Object[] args, String defaultMessage);

    /**
     * Get the specified message with the default Locale.
     *
     * @param code the message code to retrieve
     * @param args the arguments to replace
     * @return the message string with parameters replaced
     */
    public String getMessage(String code, Object[] args);

    /**
     * Get the specified message with the default Locale.
     *
     * @param prefix the prefix value of a message code
     * @param code the message code to retrieve
     * @param args the arguments to replace
     * @param defaultMessage the default message to use is the messge is not found
     * @return the message string with parameters replaced
     */
    public String getMessage(String prefix, String code, Object[] args, String defaultMessage);

    /**
     * Get the specified message with the default Locale.
     *
     * @param prefix the prefix value of a message code
     * @param code the message code to retrieve
     * @param args the arguments to replace
     * @return the message string with parameters replaced
     */
    public String getMessage(String prefix, String code, Object[] args);

}
