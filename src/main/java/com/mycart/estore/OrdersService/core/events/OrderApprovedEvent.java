package com.mycart.estore.OrdersService.core.events;

import com.mycart.estore.OrdersService.core.model.OrderStatus;
import lombok.Value;

@Value
public class OrderApprovedEvent {
    private final String orderId;
    private final OrderStatus orderStatus = OrderStatus.APPROVED;
}
