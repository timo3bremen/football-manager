package com.example.manager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a financial transaction for a team
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "amount", nullable = false)
    private Long amount; // positive for income, negative for expense

    @Column(name = "type", nullable = false)
    private String type; // 'income', 'expense'

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category; // 'attendance', 'sponsors', 'competition', 'salaries', 'infrastructure', 'interest', 'transfers'

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(Long teamId, Long amount, String type, String description, String category) {
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
