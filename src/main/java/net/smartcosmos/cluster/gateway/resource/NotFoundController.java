package net.smartcosmos.cluster.gateway.resource;

import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.smartcosmos.cluster.gateway.domain.ErrorResponse;

@Slf4j
@RestController
@PreAuthorize("permitAll()")
public class NotFoundController {

    public static final String NOT_FOUND_PATH = "/notFound";

    private static final String REQUEST_URI_KEY = "requestURI";

    @RequestMapping(value = NOT_FOUND_PATH + "/**")
    public ResponseEntity<?> notFound() {

        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(ErrorResponse.builder()
                      .timestamp(System.currentTimeMillis())
                      .status(status.value())
                      .error(status.getReasonPhrase())
                      .path(getRequestUri())
                      .build());
    }

    private String getRequestUri() {

        RequestContext context = RequestContext.getCurrentContext();

        if (context.containsKey(REQUEST_URI_KEY)) {
            return context.get(REQUEST_URI_KEY)
                .toString();
        }

        return context.getRequest()
            .getServletPath()
            .replace(NOT_FOUND_PATH, "");
    }
}
