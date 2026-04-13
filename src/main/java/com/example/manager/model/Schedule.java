package com.example.manager.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete schedule/season with 38 matchdays (double round-robin).
 */
@Entity
@Table(name = "schedules")
public class Schedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "leagueId")
	private Long leagueId;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "scheduleId")
	private List<Matchday> matchdays = new ArrayList<>();

	public Schedule() {
	}

	public Schedule(Long leagueId) {
		this.leagueId = leagueId;
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

	public List<Matchday> getMatchdays() {
		return matchdays;
	}

	public void setMatchdays(List<Matchday> matchdays) {
		this.matchdays = matchdays;
	}

	@Override
	public String toString() {
		return "Schedule{" + "id=" + id + ", leagueId=" + leagueId + ", matchdays=" + matchdays.size() + '}';
	}
}
