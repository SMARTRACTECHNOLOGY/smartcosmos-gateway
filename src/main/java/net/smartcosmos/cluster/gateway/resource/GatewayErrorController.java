package net.smartcosmos.cluster.gateway.resource;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

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
    public static final String ZUUL_REQUEST_URI = "requestURI";

    @RequestMapping(value = ERROR_PATH)
    public ResponseEntity<?> error() {

        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            HttpServletRequest request = ctx.getRequest();
            String requestUri = "unknown-uri";
            if (ctx != null && ctx.get(ZUUL_REQUEST_URI) != null) {
                requestUri = (String) ctx.get(ZUUL_REQUEST_URI);
            }

            String statusCodeString = "Unknown";
            int statusCode = 0;
            if (ctx.containsKey(ATTR_ERROR_STATUS_CODE)) {
                statusCode = (Integer) ctx.get(ATTR_ERROR_STATUS_CODE);
                statusCodeString = String.valueOf(statusCode);
            }

            String exceptionMessage = "No exception in context";
            Object errorException = null;
            if (ctx.containsKey(ATTR_ERROR_EXCEPTION)) {
                errorException = ctx.get(ATTR_ERROR_EXCEPTION);
                if (errorException != null) {
                    exceptionMessage = errorException.toString();
                }
            }

            String errorMessage = "No message available";
            if (ctx != null && ctx.containsKey(ATTR_ERROR_MESSAGE)) {
                errorMessage = (String) ctx.get(ATTR_ERROR_MESSAGE);
            }

            String msg = String.format("An exception was encountered processing statusCode: '%s', '%s', message: '%s', cause: '%s'.", statusCode,
                                       requestUri, errorMessage, exceptionMessage);
            log.warn(msg);
            log.debug(msg, errorException);
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .build();
        } catch (Throwable t) {
            log.info("Exception, cause: {}", t.toString());
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(t.toString());
        }
    }

    @Override
    public String getErrorPath() {

        return ERROR_PATH;
    }
}
