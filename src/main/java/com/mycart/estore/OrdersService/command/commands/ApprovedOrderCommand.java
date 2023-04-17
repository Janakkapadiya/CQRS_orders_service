package com.mycart.estore.OrdersService.command.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
public class ApprovedOrderCommand {
    @TargetAggregateIdentifier
    private final String orderId;
}
