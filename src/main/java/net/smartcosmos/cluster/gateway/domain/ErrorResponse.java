package net.smartcosmos.cluster.gateway.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Long timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
}
