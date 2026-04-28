package com.example.manager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Game state tracking: current matchday and last simulation time
 */
@Entity
@Table(name = "game_state_tracking")
public class GameStateTracking {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "current_matchday")
	private int currentMatchday = 1;

	@Column(name = "last_simulation_time")
	private long lastSimulationTime = 0;

	@Column(name = "last_interest_matchday")
	private int lastInterestMatchday = 0;

	public GameStateTracking() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getCurrentMatchday() {
		return currentMatchday;
	}

	public void setCurrentMatchday(int currentMatchday) {
		this.currentMatchday = currentMatchday;
	}

	public long getLastSimulationTime() {
		return lastSimulationTime;
	}

	public void setLastSimulationTime(long lastSimulationTime) {
		this.lastSimulationTime = lastSimulationTime;
	}

	public int getLastInterestMatchday() {
		return lastInterestMatchday;
	}

	public void setLastInterestMatchday(int lastInterestMatchday) {
		this.lastInterestMatchday = lastInterestMatchday;
	}
}
