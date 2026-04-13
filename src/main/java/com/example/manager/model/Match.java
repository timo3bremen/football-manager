package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Represents a single match between two teams on a specific matchday.
 */
@Entity
@Table(name = "matches")
public class Match {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "matchdayId")
	private Long matchdayId;

	@Column(name = "homeTeamId")
	private Long homeTeamId; // null if no team assigned

	@Column(name = "awayTeamId")
	private Long awayTeamId; // null if no team assigned

	private Integer homeGoals; // null = not played yet
	private Integer awayGoals;

	private String status; // "scheduled", "played", "cancelled"

	public Match() {
	}

	public Match(Long matchdayId, Long homeTeamId, Long awayTeamId) {
		this.matchdayId = matchdayId;
		this.homeTeamId = homeTeamId;
		this.awayTeamId = awayTeamId;
		this.status = "scheduled";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMatchdayId() {
		return matchdayId;
	}

	public void setMatchdayId(Long matchdayId) {
		this.matchdayId = matchdayId;
	}

	public Long getHomeTeamId() {
		return homeTeamId;
	}

	public void setHomeTeamId(Long homeTeamId) {
		this.homeTeamId = homeTeamId;
	}

	public Long getAwayTeamId() {
		return awayTeamId;
	}

	public void setAwayTeamId(Long awayTeamId) {
		this.awayTeamId = awayTeamId;
	}

	public Integer getHomeGoals() {
		return homeGoals;
	}

	public void setHomeGoals(Integer homeGoals) {
		this.homeGoals = homeGoals;
	}

	public Integer getAwayGoals() {
		return awayGoals;
	}

	public void setAwayGoals(Integer awayGoals) {
		this.awayGoals = awayGoals;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Match{" + "id=" + id + ", homeTeamId=" + homeTeamId + ", awayTeamId=" + awayTeamId + ", homeGoals="
				+ homeGoals + ", awayGoals=" + awayGoals + ", status='" + status + '\'' + '}';
	}
}
