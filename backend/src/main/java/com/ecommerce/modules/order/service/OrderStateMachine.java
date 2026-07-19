package com.ecommerce.modules.order.service;

import com.ecommerce.common.enums.OrderStatus;
import com.ecommerce.common.exception.StateMachineException;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.PENDING_PAY, Set.of(OrderStatus.PAID, OrderStatus.CANCELED, OrderStatus.CLOSED));
        TRANSITIONS.put(OrderStatus.PAID, Set.of(OrderStatus.SHIPPED, OrderStatus.REFUNDING));
        TRANSITIONS.put(OrderStatus.SHIPPED, Set.of(OrderStatus.COMPLETED, OrderStatus.REFUNDING));
        TRANSITIONS.put(OrderStatus.REFUNDING, Set.of(OrderStatus.REFUNDED, OrderStatus.REJECTED));
        TRANSITIONS.put(OrderStatus.COMPLETED, Set.of());
        TRANSITIONS.put(OrderStatus.CANCELED, Set.of());
        TRANSITIONS.put(OrderStatus.CLOSED, Set.of());
        TRANSITIONS.put(OrderStatus.REFUNDED, Set.of());
        TRANSITIONS.put(OrderStatus.REJECTED, Set.of());
    }

    public static OrderStatus transition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new StateMachineException(from.getCode(), "->" + to.getCode());
        }
        return to;
    }

    public static boolean canTransition(String fromCode, String toCode) {
        try {
            transition(OrderStatus.of(fromCode), OrderStatus.of(toCode));
            return true;
        } catch (StateMachineException e) {
            return false;
        }
    }
}
