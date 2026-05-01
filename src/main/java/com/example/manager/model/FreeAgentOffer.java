package com.example.manager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Angebot eines Teams für einen freien Spieler
 */
@Entity
@Table(name = "free_agent_offers")
public class FreeAgentOffer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "playerId")
	private Long playerId;

	@Column(name = "teamId")
	private Long teamId;

	@Column(name = "salary")
	private Long salary;

	@Column(name = "contractLength")
	private Integer contractLength;

	@Column(name = "status")
	private String status = "pending"; // pending, accepted, outbid

	@Column(name = "createdAt")
	private LocalDateTime createdAt;

	public FreeAgentOffer() {
	}

	public FreeAgentOffer(Long playerId, Long teamId, Long salary, Integer contractLength) {
		this.playerId = playerId;
		this.teamId = teamId;
		this.salary = salary;
		this.contractLength = contractLength;
		this.status = "pending";
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public Long getSalary() {
		return salary;
	}

	public void setSalary(Long salary) {
		this.salary = salary;
	}

	public Integer getContractLength() {
		return contractLength;
	}

	public void setContractLength(Integer contractLength) {
		this.contractLength = contractLength;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
