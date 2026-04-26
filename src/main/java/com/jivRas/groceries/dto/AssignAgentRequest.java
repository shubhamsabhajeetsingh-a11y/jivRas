package com.jivRas.groceries.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Request body for PATCH /api/orders/{orderId}/assign-agent.
 * Sent by an admin or branch manager to assign a delivery agent to an order.
 */
@Data
public class AssignAgentRequest {

    /** Numeric ID of the EmployeeUser with role = DELIVERY_AGENT to assign. */
    private Long agentId;

    /** Admin-set estimated delivery time displayed on the agent's dashboard. */
    private LocalDateTime estimatedDeliveryTime;
}
