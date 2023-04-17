/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycart.estore.OrdersService.command;

import com.mycart.estore.OrdersService.command.commands.ApprovedOrderCommand;
import com.mycart.estore.OrdersService.command.commands.RejectOrderCommand;
import com.mycart.estore.OrdersService.core.events.OrderApprovedEvent;
import com.mycart.estore.OrdersService.core.events.OrderCreatedEvent;
import com.mycart.estore.OrdersService.core.events.OrderRejectedEvent;
import com.mycart.estore.OrdersService.core.model.OrderStatus;
import com.mycart.estore.OrdersService.command.commands.CreateOrderCommand;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

@Aggregate
public class OrderAggregate {

    @AggregateIdentifier
    private String orderId;
    private String productId;
    private String userId;
    private int quantity;
    private String addressId;
    private OrderStatus orderStatus;

    public OrderAggregate() {
    }

    @CommandHandler
    public OrderAggregate(CreateOrderCommand createOrderCommand) {
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent();
        BeanUtils.copyProperties(createOrderCommand, orderCreatedEvent);

        AggregateLifecycle.apply(orderCreatedEvent);
    }

    @EventSourcingHandler
    public void on(OrderCreatedEvent orderCreatedEvent){
        this.orderId = orderCreatedEvent.getOrderId();
        this.productId = orderCreatedEvent.getProductId();
        this.userId = orderCreatedEvent.getUserId();
        this.addressId = orderCreatedEvent.getAddressId();
        this.quantity = orderCreatedEvent.getQuantity();
        this.orderStatus = orderCreatedEvent.getOrderStatus();
    }

    @CommandHandler
    public void handle(ApprovedOrderCommand approvedOrderCommand) {
        // create apd publish the order approved event
        OrderApprovedEvent orderApprovedEvent = new OrderApprovedEvent(approvedOrderCommand.getOrderId());
        AggregateLifecycle.apply(orderApprovedEvent);
    }

    @EventSourcingHandler
    public void on(OrderApprovedEvent orderApprovedEvent) {
       this.orderStatus = orderApprovedEvent.getOrderStatus();
    }

    @CommandHandler
    public void handle(RejectOrderCommand rejectOrderCommand){
        OrderRejectedEvent orderRejectedEvent = new OrderRejectedEvent(rejectOrderCommand.getOrderId(),
                rejectOrderCommand.getReason()
                );
        AggregateLifecycle.apply(orderRejectedEvent);

    }

    @EventSourcingHandler
    public void on(OrderRejectedEvent rejectOrderRejectedEvent)
    {
        this.orderStatus = rejectOrderRejectedEvent.getOrderStatus();
    }
}
