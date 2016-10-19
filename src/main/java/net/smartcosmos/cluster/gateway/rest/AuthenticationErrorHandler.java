package net.smartcosmos.cluster.gateway.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Error Handler component for requests to the Auth Server.
 */
@Component
@Slf4j
public class AuthenticationErrorHandler extends DefaultResponseErrorHandler {

    private static final String JSON_ERROR_DESCRIPTION = "error_description";

    /**
     * <p>The error handler intercepts the default error handler and checks if the returned HTTP status code is <i>400 Bad Request</i>. This status
     * code particularly indicates that the authentication failure was not caused by the client or services, but was caused by invalid credentials
     * or other account-related issues (e.g. account disabled).</p>
     * <p>The method attempts to read the error description from the response body.</p>
     *
     * @param response the response
     * @throws BadCredentialsException if the HTTP status code was <i>400 Bad Request</i>
     * @throws IOException             in case of I/O errors
     */
    @Override
    public void handleError(ClientHttpResponse response) throws BadCredentialsException, IOException {

        if (BAD_REQUEST.equals(response.getStatusCode())) {
            String responseBody = getResponseBody(response);
            throw new BadCredentialsException(getErrorDescriptionFromBody(responseBody));
        }

        super.handleError(response);
    }

    protected String getResponseBody(ClientHttpResponse response) {

        try {
            InputStream responseBody = response.getBody();
            if (responseBody != null) {
                return new String(FileCopyUtils.copyToByteArray(responseBody));
            }
        } catch (IOException ex) {
            // ignore
        }
        return "";
    }

    protected String getErrorDescriptionFromBody(String body) {

        log.debug("Attempt to read error description from response body '{}'", body);

        if (StringUtils.isNotBlank(body)) {
            String[] jsonEntries = body.replace("{", "")
                .replace("}", "")
                .replace("\"", "")
                .split(",");

            Map<String, String> jsonMap = Arrays.stream(jsonEntries)
                .filter(entry -> entry.contains(":"))
                .map(entry -> entry.split(":"))
                .collect(Collectors.toMap(stringArray -> stringArray[0], array -> array[1]));

            if (jsonMap.containsKey(JSON_ERROR_DESCRIPTION)) {
                return jsonMap.get(JSON_ERROR_DESCRIPTION);
            }
        }

        return "Invalid username or password";
    }
}
