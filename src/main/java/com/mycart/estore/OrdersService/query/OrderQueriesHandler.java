package com.mycart.estore.OrdersService.query;

import com.mycart.estore.OrdersService.core.data.OrderEntity;
import com.mycart.estore.OrdersService.core.data.OrdersRepository;
import com.mycart.estore.OrdersService.core.model.OrderSummary;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderQueriesHandler {

	OrdersRepository ordersRepository;

	public OrderQueriesHandler(OrdersRepository ordersRepository) {
		this.ordersRepository = ordersRepository;
	}

	@QueryHandler
	public OrderSummary findOrder(FindOrderQuery findOrderQuery) {
		OrderEntity orderEntity = ordersRepository.findByOrderId(findOrderQuery.getOrderId());
		return new OrderSummary(orderEntity.getOrderId(),
				orderEntity.getOrderStatus(), "");
	}

}
