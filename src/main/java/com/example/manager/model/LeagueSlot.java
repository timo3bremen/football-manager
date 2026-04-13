package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Represents a single slot in a league (position 1-20).
 * Each slot can hold one team or be empty (teamId = null).
 */
@Entity
@Table(name = "league_slots")
public class LeagueSlot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "leagueId")
	private Long leagueId;

	private int position; // 1-20

	@Column(name = "teamId")
	private Long teamId; // null if empty

	public LeagueSlot() {
	}

	public LeagueSlot(Long leagueId, int position) {
		this.leagueId = leagueId;
		this.position = position;
		this.teamId = null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getLeagueId() {
		return leagueId;
	}

	public void setLeagueId(Long leagueId) {
		this.leagueId = leagueId;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	@Override
	public String toString() {
		return "LeagueSlot{" + "id=" + id + ", position=" + position + ", teamId=" + teamId + '}';
	}
}
