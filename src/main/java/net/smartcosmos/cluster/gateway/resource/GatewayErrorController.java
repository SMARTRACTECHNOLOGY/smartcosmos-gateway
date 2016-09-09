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

    private static final String ERROR_PATH = "/error";
    private static final String ATTR_ERROR_EXCEPTION = "error.exception";
    public static final String ATTR_ERROR_MESSAGE = "error.message";
    public static final String ATTR_JAVAX_SERVLET_ERROR_STATUS_CODE = "javax.servlet.error.status_code";
    public static final String ATTR_JAVAX_SERVLET_ERROR_MESSAGE = "javax.servlet.error.message";
    public static final String ATTR_ERROR_STATUS_CODE = "error.status_code";

    @RequestMapping(value = ERROR_PATH)
    public ResponseEntity<?> error() {

        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            HttpServletRequest request = ctx.getRequest();

            int statusCode = (Integer) ctx.get(ATTR_ERROR_STATUS_CODE);

            if (ctx.containsKey(ATTR_ERROR_EXCEPTION)) {
                Object errorException = ctx.get(ATTR_ERROR_EXCEPTION);
            }

            if (ctx.containsKey(ATTR_ERROR_MESSAGE)) {
                String message = (String) ctx.get(ATTR_ERROR_MESSAGE);
                request.setAttribute(ATTR_JAVAX_SERVLET_ERROR_MESSAGE, message);
            }

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
