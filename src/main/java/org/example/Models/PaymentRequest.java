package org.example.Models;

public class PaymentRequest {
    private Long order_id;
    private String method;

    public Long getOrderId() { return order_id; }
    public void setOrderId(Long orderId) { this.order_id = orderId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}