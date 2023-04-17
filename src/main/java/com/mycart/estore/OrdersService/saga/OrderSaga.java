package com.mycart.estore.OrdersService.saga;

import com.mycart.estore.OrdersService.command.commands.ApprovedOrderCommand;
import com.mycart.estore.OrdersService.command.commands.RejectOrderCommand;
import com.mycart.estore.OrdersService.core.events.OrderApprovedEvent;
import com.mycart.estore.OrdersService.core.events.OrderCreatedEvent;
import com.mycart.estore.OrdersService.core.events.OrderRejectedEvent;
import com.mycart.estore.OrdersService.core.model.OrderSummary;
import com.mycart.estore.OrdersService.query.FindOrderQuery;
import com.mycart.estore.core.commands.CancelReservedProductCommand;
import com.mycart.estore.core.commands.ProcessPaymentCommand;
import com.mycart.estore.core.commands.ReserveProductCommand;
import com.mycart.estore.core.events.ProcessPaymentEvent;
import com.mycart.estore.core.events.ProductReservationCancelledEvent;
import com.mycart.estore.core.events.ProductReservedEvent;
import com.mycart.estore.core.model.User;
import com.mycart.estore.core.query.FetchUserPaymentDetailsQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;

import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Saga
@Slf4j
public class OrderSaga {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;
    @Autowired
    private transient DeadlineManager deadlineManager;
    @Autowired
    private transient QueryUpdateEmitter queryUpdateEmitter;
    private String scheduleId;

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
                RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(orderCreatedEvent.getOrderId(),
                        commandResultMessage.exceptionResult().getMessage()
                );
                commandGateway.send(rejectOrderCommand);
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
            log.error("no payment details found {}", e.getMessage());
            cancelProductReservation(productReservedEvent, e.getMessage());
        }

        if (paymentDetails == null) {
            cancelProductReservation(productReservedEvent, "could not fetch user payment details");
            return;
        }

        scheduleId = deadlineManager.schedule(Duration.of(120, ChronoUnit.SECONDS), "payment-process-deadline", productReservedEvent);

        if(true) return;

        ProcessPaymentCommand processPayment = ProcessPaymentCommand.builder()
                .orderId(productReservedEvent.getOrderId())
                .paymentId(UUID.randomUUID().toString())
                .paymentDetails(paymentDetails.getPaymentDetails())
                .build();

        String result = null;

        try {
            result = commandGateway.sendAndWait(processPayment);
        } catch (Exception e) {
            log.error("payment process resulted failed {}", e.getMessage());
            cancelProductReservation(productReservedEvent, e.getMessage());
            return;
        }

        if (result == null) {
            log.error("payment process command resulted NULL");
            cancelProductReservation(productReservedEvent, "could not process user payment with provided payment details");
        }
    }

    private void cancelProductReservation(ProductReservedEvent product, String reason) {

        cancelDeadline();

        CancelReservedProductCommand cancelReservedProductCommand =
                CancelReservedProductCommand.builder()
                        .productId(product.getProductId())
                        .orderId(product.getOrderId())
                        .quantity(product.getQuantity())
                        .userId(product.getUserId())
                        .reason(reason)
                        .build();

        commandGateway.send(cancelReservedProductCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProcessPaymentEvent processPaymentEvent) {
        cancelDeadline();
        ApprovedOrderCommand approvedOrderCommand = new ApprovedOrderCommand(processPaymentEvent.getOrderId());
        commandGateway.send(approvedOrderCommand);
    }

    private void cancelDeadline() {
        if(scheduleId != null) {
            deadlineManager.cancelSchedule("payment-process-deadline", scheduleId);
            scheduleId = null;
        }
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderApprovedEvent orderApprovedEvent) {
        log.info("order is approved. Order saga is completed for orderId {}", orderApprovedEvent.getOrderId());
        queryUpdateEmitter.emit(FindOrderQuery.class,query -> true,
                  new OrderSummary(orderApprovedEvent.getOrderId(), orderApprovedEvent.getOrderStatus(),""));
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservationCancelledEvent productReservationCancelledEvent){
        // send a rejectOrderCommand
        RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(productReservationCancelledEvent.getOrderId(),
                productReservationCancelledEvent.getReason()
                );
        commandGateway.send(rejectOrderCommand);
    }
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderRejectedEvent orderRejectedEvent){
        log.info("order with {} id has been rejected",orderRejectedEvent.getOrderId());
        queryUpdateEmitter.emit(FindOrderQuery.class,query -> true,
                new OrderSummary(orderRejectedEvent.getOrderId(),
                        orderRejectedEvent.getOrderStatus(),orderRejectedEvent.getReason()));
    }

    @DeadlineHandler(deadlineName = "payment-process-deadline")
    public void handlePaymentDeadline(ProductReservedEvent productReservedEvent){
        log.info("payment process deadline took place");
        cancelProductReservation(productReservedEvent,"payment timeout");
    }
}
