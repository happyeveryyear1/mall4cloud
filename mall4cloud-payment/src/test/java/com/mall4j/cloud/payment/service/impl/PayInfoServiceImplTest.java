package com.mall4j.cloud.payment.service.impl;

import com.mall4j.cloud.api.auth.bo.UserInfoInTokenBO;
import com.mall4j.cloud.common.security.AuthUserContext;
import com.mall4j.cloud.payment.controller.PayController;
import com.mall4j.cloud.payment.mapper.PayInfoMapper;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class PayInfoServiceImplTest {

    @Test
    public void invalidPaymentLookupsAreUnpaidWithoutQuerying() throws Exception {
        PayInfoServiceImpl service = service((proxy, method, args) -> {
            throw new AssertionError("Invalid payment lookup must not reach SQL: " + method.getName());
        });
        assertEquals(Integer.valueOf(0), service.isPay(null, 41L));
        assertEquals(Integer.valueOf(0), service.isPay(" \t ", 41L));
        assertEquals(Integer.valueOf(0), service.isPay("7,8", null));
        assertEquals(Integer.valueOf(0), service.isPay("7,8", 0L));
        assertEquals(Integer.valueOf(0), service.isPay("7,8", -1L));
    }

    @Test
    public void missingPaymentCountIsUnpaid() throws Exception {
        assertEquals(Integer.valueOf(0), serviceWithCount(null).isPay("7,8", 41L));
    }

    @Test
    public void zeroPaymentsAreUnpaid() throws Exception {
        assertEquals(Integer.valueOf(0), serviceWithCount(0).isPay("7,8", 41L));
    }

    @Test
    public void multiplePaidRowsStillMeanPaid() throws Exception {
        assertEquals(Integer.valueOf(1), serviceWithCount(2).isPay("7,8", 41L));
    }

    @Test
    public void trimsOuterWhitespaceBeforeLookingUpTheAuthenticatedUser() throws Exception {
        assertEquals(Integer.valueOf(1), serviceWithCount(1).isPay(" 7,8 \t", 41L));
    }

    @Test
    public void controllerReturnsPaidWithoutTheUnusedStatusQuery() throws Exception {
        assertControllerResponse(2, true);
    }

    @Test
    public void controllerReturnsUnpaidForNoMatchingPayment() throws Exception {
        assertControllerResponse(0, false);
    }

    private void assertControllerResponse(Integer count, boolean expected) throws Exception {
        PayController controller = new PayController();
        Field field = PayController.class.getDeclaredField("payInfoService");
        field.setAccessible(true);
        field.set(controller, serviceWithCount(count));
        UserInfoInTokenBO user = new UserInfoInTokenBO();
        user.setUserId(41L);
        AuthUserContext.set(user);
        try {
            assertEquals(Boolean.valueOf(expected), controller.isPay("7,8").getBody());
        } finally {
            AuthUserContext.clean();
        }
    }

    private PayInfoServiceImpl serviceWithCount(Integer count) throws Exception {
        return service((proxy, method, args) -> {
            assertEquals("isPay", method.getName());
            assertEquals("7,8", args[0]);
            assertEquals(Long.valueOf(41), args[1]);
            return count;
        });
    }

    private PayInfoServiceImpl service(InvocationHandler handler) throws Exception {
        PayInfoMapper mapper = (PayInfoMapper) Proxy.newProxyInstance(
                PayInfoMapper.class.getClassLoader(), new Class<?>[]{PayInfoMapper.class}, handler);
        PayInfoServiceImpl service = new PayInfoServiceImpl();
        Field field = PayInfoServiceImpl.class.getDeclaredField("payInfoMapper");
        field.setAccessible(true);
        field.set(service, mapper);
        return service;
    }
}
