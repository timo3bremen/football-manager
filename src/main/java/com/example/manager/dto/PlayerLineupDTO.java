package com.example.manager.dto;

/**
 * DTO for a player in a lineup.
 */
public class PlayerLineupDTO {
	private Long playerId;
	private String playerName;
	private String position;
	private int rating;
	private int slotName;

	public PlayerLineupDTO(Long playerId, String playerName, String position, int rating, int slotName) {
		this.playerId = playerId;
		this.playerName = playerName;
		this.position = position;
		this.rating = rating;
		this.slotName = slotName;
	}

	// Getters and Setters
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

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public int getSlotName() {
		return slotName;
	}

	public void setSlotName(int slotName) {
		this.slotName = slotName;
	}
}
