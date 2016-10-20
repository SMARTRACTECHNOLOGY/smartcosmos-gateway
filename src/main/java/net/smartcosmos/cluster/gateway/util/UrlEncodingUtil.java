package net.smartcosmos.cluster.gateway.util;

import java.io.UnsupportedEncodingException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.CharEncoding;
import org.springframework.web.util.UriUtils;

@Slf4j
public class UrlEncodingUtil {

    /**
     * Applies UTF-8 URL encoding to a provided String.
     *
     * @param value the input String
     * @return the encoded value
     */
    public static String encode(String value) {

        try {
            return UriUtils.encode(value, CharEncoding.UTF_8);
        } catch (UnsupportedEncodingException e) {
            log.warn("UTF-8 encoding is not supported");
        }

        // If we can't encode with UTF-8, return the original value
        return value;
    }
}
