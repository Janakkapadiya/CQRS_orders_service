package com.mycart.estore.OrdersService.command.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@Builder
public class RejectOrderCommand {
    @TargetAggregateIdentifier
    private final String orderId;
    private final String reason;
}
