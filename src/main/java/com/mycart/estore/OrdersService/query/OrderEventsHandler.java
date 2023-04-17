/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycart.estore.OrdersService.query;

import com.mycart.estore.OrdersService.core.data.OrderEntity;
import com.mycart.estore.OrdersService.core.data.OrdersRepository;
import com.mycart.estore.OrdersService.core.events.OrderApprovedEvent;
import com.mycart.estore.OrdersService.core.events.OrderCreatedEvent;

import com.mycart.estore.OrdersService.core.events.OrderRejectedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("order-group")
public class OrderEventsHandler {
    
    private final OrdersRepository ordersRepository;
    
    public OrderEventsHandler(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;
    }

    @EventHandler
    public void on(OrderCreatedEvent event){
        OrderEntity orderEntity = new OrderEntity();
        BeanUtils.copyProperties(event, orderEntity);
 
        ordersRepository.save(orderEntity);
    }

    @EventHandler
    public void on(OrderApprovedEvent orderApprovedEvent){
        OrderEntity order = ordersRepository.findByOrderId(orderApprovedEvent.getOrderId());

        if(order == null) {
            return;
        }
        order.setOrderStatus(order.getOrderStatus());

        ordersRepository.save(order);
    }

    @EventHandler
    public void on(OrderRejectedEvent orderRejectedEvent){
        OrderEntity order = ordersRepository.findByOrderId(orderRejectedEvent.getOrderId());
        order.setOrderStatus(orderRejectedEvent.getOrderStatus());
        ordersRepository.save(order);
    }

}
