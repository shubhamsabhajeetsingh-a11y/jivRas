package com.jivRas.groceries.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for POST /api/permissions and PUT /api/permissions/{id}.
 */
@Data
public class RolePermissionRequest {

    @NotBlank(message = "role is required")
    private String role;

    @NotBlank(message = "endpoint is required")
    private String endpoint;

    /** Accepted values: GET, POST, PUT, DELETE, PATCH, * */
    @NotBlank(message = "httpMethod is required")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH|\\*)$",
             message = "httpMethod must be one of GET, POST, PUT, DELETE, PATCH, or *")
    private String httpMethod;

    private boolean isAllowed = true;
}
