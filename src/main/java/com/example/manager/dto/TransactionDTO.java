package com.example.manager.dto;

import java.time.LocalDateTime;

/**
 * DTO for financial transactions
 */
public class TransactionDTO {
    
    private Long id;
    private Long teamId;
    private Long amount;
    private String type;
    private String description;
    private String category; // 'attendance', 'sponsors', 'competition', 'salaries', 'infrastructure', 'interest', 'transfers'
    private LocalDateTime createdAt;

    public TransactionDTO() {
    }

    public TransactionDTO(Long teamId, Long amount, String type, String description, String category) {
        this.teamId = teamId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.category = category;
        this.createdAt = LocalDateTime.now();
    }

    // ...existing code...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
