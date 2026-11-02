package com.mall4j.cloud.order.service.impl;

import com.mall4j.cloud.order.mapper.OrderItemMapper;
import com.mall4j.cloud.order.service.OrderItemService;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class OrderItemServiceImplTest {

    @Test
    public void listQuerySkipsNullAndDuplicateIdsAndReturnsProductNames() throws Exception {
        OrderItemService service = service((proxy, method, args) -> {
            assertArrayEquals(new long[]{7, 3}, (long[]) args[0]);
            return List.of("shirt", "book");
        });
        assertEquals(List.of("shirt", "book"), service.getSpuNameListByOrderIds(
                Arrays.asList(7L, null, 0L, 3L, 7L, -2L)));
    }

    @Test
    public void nullOrderIdsReturnNoNamesWithoutQuerying() throws Exception {
        assertTrue(serviceWithoutQueries().getSpuNameListByOrderIds(null).isEmpty());
    }

    @Test
    public void emptyOrderIdsReturnNoNamesWithoutQuerying() throws Exception {
        assertTrue(serviceWithoutQueries().getSpuNameListByOrderIds(List.of()).isEmpty());
    }

    @Test
    public void nonPositiveOrderIdsReturnNoNamesWithoutQuerying() throws Exception {
        assertTrue(serviceWithoutQueries().getSpuNameListByOrderIds(List.of(0L, -2L)).isEmpty());
    }

    @Test
    public void queriesEachPositiveOrderOnceAndReturnsItsProductNames() throws Exception {
        OrderItemServiceImpl service = service((proxy, method, args) -> {
            assertEquals("getSpuNameListByOrderIds", method.getName());
            assertArrayEquals(new long[]{7, 3}, (long[]) args[0]);
            return List.of("shirt", "book");
        });
        assertEquals(List.of("shirt", "book"),
                service.getSpuNameListByOrderIds(List.of(7L, 0L, 3L, 7L, -2L)));
    }

    private OrderItemServiceImpl serviceWithoutQueries() throws Exception {
        return service((proxy, method, args) -> {
            throw new AssertionError("Empty normalized order IDs must not reach SQL: " + method.getName());
        });
    }

    private OrderItemServiceImpl service(InvocationHandler handler) throws Exception {
        OrderItemMapper mapper = (OrderItemMapper) Proxy.newProxyInstance(
                OrderItemMapper.class.getClassLoader(), new Class<?>[]{OrderItemMapper.class}, handler);
        OrderItemServiceImpl service = new OrderItemServiceImpl();
        Field field = OrderItemServiceImpl.class.getDeclaredField("orderItemMapper");
        field.setAccessible(true);
        field.set(service, mapper);
        return service;
    }
}
