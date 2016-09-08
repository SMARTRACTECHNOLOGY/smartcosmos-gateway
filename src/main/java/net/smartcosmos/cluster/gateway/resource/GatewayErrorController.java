package net.smartcosmos.cluster.gateway.resource;

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

    @RequestMapping(value = ERROR_PATH)
    public ResponseEntity<?> error() {

        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
            .build();
    }

    @Override
    public String getErrorPath() {

        return ERROR_PATH;
    }
}
