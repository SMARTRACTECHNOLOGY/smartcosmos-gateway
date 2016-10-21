package net.smartcosmos.cluster.gateway.resource;

import java.net.SocketTimeoutException;

import com.netflix.client.ClientException;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import net.smartcosmos.cluster.gateway.domain.ErrorResponse;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 *
 */
@Slf4j
@RestController
@ResponseBody
@PreAuthorize("permitAll()")
public class GatewayErrorController implements ErrorController {

    public static final String ERROR_PATH = "/error";
    public static final String ATTR_ERROR_EXCEPTION = "error.exception";
    public static final String ATTR_ERROR_MESSAGE = "error.message";
    public static final String ATTR_ERROR_STATUS_CODE = "error.status_code";
    public static final String ATTR_PROXY = "proxy";
    public static final String ATTR_SERVICE_ID = "serviceId";
    public static final String ZUUL_REQUEST_URI = "requestURI";

    /**
     * <p>Error handling method that evalutes the current {@link RequestContext} and extracts information on the request, matching routes and error
     * or exceptions that occurred during routing.</p>
     * <p>It returns a {@link ResponseEntity} with an {@link ErrorResponse} body and one of these HTTP status codes, depending on the request
     * context information:</p>
     * <ul>
     * <li>500 Internal Server Error</li>
     * <li>503 Service Unavailable</li>
     * <li>504 Gateway Timeout</li>
     * </ul>
     *
     * @return the response entity
     */
    @RequestMapping(value = ERROR_PATH)
    public ResponseEntity<?> error() {

        String requestUri = null;
        String errorResponseMessage;

        RequestContext requestContext = RequestContext.getCurrentContext();
        if (MapUtils.isEmpty(requestContext)) {
            log.warn("A request failed without any available context information");
            return errorResponse(INTERNAL_SERVER_ERROR,
                                 "No context information available. A reason for this can be that no configured route matched the request.",
                                 null);
        }

        try {
            requestUri = getRequestUriFromRequestContext(requestContext);
            HttpStatus httpStatus = getHttpStatusFromRequestContext(requestContext);

            String route = getRouteFromRequestContext(requestContext);
            String serviceId = getServiceIdFromRequestContext(requestContext);

            String errorMessage = getErrorMessageFromRequestContext(requestContext);

            Exception errorException = getExceptionFromRequestContext(requestContext);
            String exceptionMessage = "No message available";
            Throwable rootCause = null;
            if (errorException != null) {
                exceptionMessage = errorException.getMessage();
                rootCause = ExceptionUtils.getRootCause(errorException);
            }
            if (rootCause != null && StringUtils.isNotBlank(rootCause.getMessage())) {
                exceptionMessage = exceptionMessage.concat(String.format(": %s", rootCause.getMessage()));
            }

            String msg = String.format("Using route '%s' to service '%s' for request '%s' failed.\n"
                                       + "Status code: '%s', Error: '%s'\n"
                                       + "Cause: %s\n"
                                       + "Root cause: %s",
                                       route,
                                       serviceId,
                                       requestUri,
                                       httpStatus,
                                       errorMessage,
                                       errorException != null ? errorException.toString() : "No exception available in context",
                                       rootCause != null ? rootCause.toString() : "N/A");
            log.warn(msg);
            log.debug(msg, errorException, rootCause);

            if (isGatewayTimeout(rootCause)) {
                return errorResponse(GATEWAY_TIMEOUT, exceptionMessage, requestUri);
            }

            if (isServiceUnavailable(rootCause)) {
                return errorResponse(SERVICE_UNAVAILABLE, exceptionMessage, requestUri);
            }

            errorResponseMessage = String.format("Gateway error: '%s' failed: %s", route, exceptionMessage);
        } catch (Throwable t) {
            errorResponseMessage = t.toString();
            log.info("Exception, cause: {}", errorResponseMessage);
        }

        return errorResponse(INTERNAL_SERVER_ERROR, errorResponseMessage, requestUri);
    }

    /**
     * <p>Checks if a given throwable matches any of the exception types that are expected to return a HTTP status code of <i>503 Service
     * Unavailable</i>.</p>
     * <p>In case of encountering exceptions that should return this response instead of <i>500 Internal Server Error</i>, they need to be added to
     * this method.</p>
     *
     * @param throwable the exception
     * @return {@code true} if it's an exception type  that should result in a <i>503 Service Unavailable</i> HTTP status response
     */
    protected boolean isServiceUnavailable(Throwable throwable) {

        return throwable instanceof ClientException;
    }

    /**
     * <p>Checks if a given throwable matches any of the exception types that are expected to return a HTTP status code of <i>504 Gateway
     * Timeout</i>.</p>
     * <p>In case of encountering exceptions that should return this response instead of <i>500 Internal Server Error</i>, they need to be added to
     * this method.</p>
     *
     * @param throwable the exception
     * @return {@code true} if it's an exception type  that should result in a <i>504 Gateway Timeout</i> HTTP status response
     */
    protected boolean isGatewayTimeout(Throwable throwable) {

        return throwable instanceof SocketTimeoutException;
    }

    /**
     * Builds an {@link ResponseEntity} containing a {@link ErrorResponse} body to return JSON error responses.
     *
     * @param httpStatus the HTTP status code to return
     * @param message the error message
     * @param path the request path where the error occurred
     * @return the response entity
     */
    protected ResponseEntity errorResponse(HttpStatus httpStatus, String message, String path) {

        return ResponseEntity.status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(ErrorResponse.builder()
                      .timestamp(System.currentTimeMillis())
                      .status(httpStatus.value())
                      .error(httpStatus.getReasonPhrase())
                      .message(message)
                      .path(path)
                      .build());
    }

    protected String getRequestUriFromRequestContext(RequestContext requestContext) {

        if (requestContext != null && requestContext.get(ZUUL_REQUEST_URI) != null) {
            return (String) requestContext.get(ZUUL_REQUEST_URI);
        }
        return "unknown-uri";
    }

    protected HttpStatus getHttpStatusFromRequestContext(RequestContext requestContext) {

        if (requestContext.containsKey(ATTR_ERROR_STATUS_CODE)) {
            return HttpStatus.valueOf((Integer) requestContext.get(ATTR_ERROR_STATUS_CODE));
        }
        return null;
    }

    protected String getRouteFromRequestContext(RequestContext requestContext) {

        if (requestContext.containsKey(ATTR_PROXY)) {
            return (String) requestContext.get(ATTR_PROXY);
        }
        return "unknown-route";
    }

    protected String getServiceIdFromRequestContext(RequestContext requestContext) {

        if (requestContext.containsKey(ATTR_SERVICE_ID)) {
            return (String) requestContext.get(ATTR_PROXY);
        }
        return "unknown-service";
    }

    protected Exception getExceptionFromRequestContext(RequestContext requestContext) {

        if (requestContext.containsKey(ATTR_ERROR_EXCEPTION)) {
            return (Exception) requestContext.get(ATTR_ERROR_EXCEPTION);
        }
        return null;
    }

    protected String getErrorMessageFromRequestContext(RequestContext requestContext) {

        if (requestContext != null && requestContext.containsKey(ATTR_ERROR_MESSAGE)) {
            return (String) requestContext.get(ATTR_ERROR_MESSAGE);
        }
        return "No message available";
    }

    protected RequestContext getCurrentContext() {

        // moved to its own method, so that it can be mocked in tests
        return RequestContext.getCurrentContext();
    }

    @Override
    public String getErrorPath() {

        return ERROR_PATH;
    }
}
