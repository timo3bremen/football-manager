package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Represents an event in a match (goal, card, etc.)
 */
@Entity
@Table(name = "match_events")
public class MatchEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "matchId")
	private Long matchId;

	@Column(name = "teamId")
	private Long teamId;

	@Column(name = "playerId")
	private Long playerId;

	private String playerName;
	private String type; // "goal", "yellow_card", "red_card"
	
	@Column(name = "game_minute")
	private int minute; // 1-90

	public MatchEvent() {
	}

	public MatchEvent(Long matchId, Long teamId, Long playerId, String playerName, String type, int minute) {
		this.matchId = matchId;
		this.teamId = teamId;
		this.playerId = playerId;
		this.playerName = playerName;
		this.type = type;
		this.minute = minute;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMatchId() {
		return matchId;
	}

	public void setMatchId(Long matchId) {
		this.matchId = matchId;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getMinute() {
		return minute;
	}

	public void setMinute(int minute) {
		this.minute = minute;
	}

	@Override
	public String toString() {
		return "MatchEvent{" + "id=" + id + ", matchId=" + matchId + ", playerId=" + playerId + ", playerName='"
				+ playerName + '\'' + ", type='" + type + '\'' + ", minute=" + minute + '}';
	}
}
