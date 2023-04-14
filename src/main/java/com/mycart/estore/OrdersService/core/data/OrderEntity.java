/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycart.estore.OrdersService.core.data;

import com.mycart.estore.OrdersService.core.model.OrderStatus;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class OrderEntity implements Serializable {

    public static final long serialVersionUID = -1275453453766313659L;

    @Id
    @Column(unique = true)
    public String orderId;
    private String productId;
    private String userId;
    private int quantity;
    private String addressId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
}
