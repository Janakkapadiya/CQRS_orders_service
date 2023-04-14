package com.mycart.estore.OrdersService.saga;

import com.mycart.estore.OrdersService.core.events.OrderCreatedEvent;
import com.mycart.estore.core.commands.ProcessPaymentCommand;
import com.mycart.estore.core.commands.ReserveProductCommand;
import com.mycart.estore.core.events.ProductReservedEvent;
import com.mycart.estore.core.model.User;
import com.mycart.estore.core.query.FetchUserPaymentDetailsQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Saga
@Slf4j
public class OrderSaga {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCreatedEvent orderCreatedEvent) {
        ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
                .orderId(orderCreatedEvent.getOrderId())
                .productId(orderCreatedEvent.getProductId())
                .quantity(orderCreatedEvent.getQuantity())
                .userId(orderCreatedEvent.getUserId())
                .build();
        log.info("OrderCreatedEvent handled for orderId {} and productId {}", reserveProductCommand.getOrderId()
                , reserveProductCommand.getProductId()
        );
        commandGateway.send(reserveProductCommand, (commandMessage, commandResultMessage) -> {
            if (commandResultMessage.isExceptional()) {
                // start compenseting transaction
            }
        });
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservedEvent productReservedEvent) throws ExecutionException, InterruptedException {
        log.info("ProductReserveEvent is handled for productId {} and orderId {}", productReservedEvent.getProductId(),
                productReservedEvent.getOrderId());

        FetchUserPaymentDetailsQuery fetchUserPayment = new FetchUserPaymentDetailsQuery(
                productReservedEvent.getUserId()
        );

        User paymentDetails = null;

        try {
            paymentDetails = queryGateway.query(fetchUserPayment, ResponseTypes.instanceOf(User.class)).join();
            log.info("successfully fetched payment details {}", paymentDetails.getFirstName());
        } catch (Exception e) {
            log.error("no payment details found {}",e.getStackTrace());
        }

        if(paymentDetails == null){
            return;
        }

        ProcessPaymentCommand processPayment = ProcessPaymentCommand.builder()
                .orderId(productReservedEvent.getOrderId())
                .paymentId(UUID.randomUUID().toString())
                .paymentDetails(paymentDetails.getPaymentDetails())
                .build();

        String result = null;

        try {
            result = commandGateway.sendAndWait(processPayment,10, TimeUnit.SECONDS);
        }catch (Exception e)
        {
            log.error("payment process resulted failed",e.getStackTrace());
            // start compenseting transaction
        }

        if(result == null){
            log.error("payment process command resulted NULL");
            // start compenseting transaction
        }
    }
}
