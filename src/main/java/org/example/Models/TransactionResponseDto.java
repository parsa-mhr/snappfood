package org.example.Models;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponseDto {
    private Long id;
    private Long userId;
    private Long orderId;
    private String method;
    private String status;
    private BigDecimal amount;
    private String createdAt;

    public TransactionResponseDto(Transaction transaction) {
        this.id = transaction.getId();
        this.userId = transaction.getUser().getId();
        this.orderId = transaction.getOrderId();
        this.method = transaction.getMethod();
        this.status = transaction.getStatus();
        this.amount = transaction.getAmount();
        this.createdAt = transaction.getCreatedAt().toString();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt.toString(); }
}