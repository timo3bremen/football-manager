package com.example.manager.dto;

import java.time.LocalDateTime;

/**
 * DTO für den aktuellen Status der Live-Simulation
 */
public class LiveSimulationStatusDTO {
	
	private boolean isRunning;
	private LocalDateTime startTime;
	private LocalDateTime expectedEndTime;
	private int currentMinute;
	private int totalDurationSeconds; // 270 Sekunden
	private long secondsRemaining;
	
	public LiveSimulationStatusDTO() {
	}
	
	public LiveSimulationStatusDTO(boolean isRunning, LocalDateTime startTime, LocalDateTime expectedEndTime, 
			int currentMinute, int totalDurationSeconds, long secondsRemaining) {
		this.isRunning = isRunning;
		this.startTime = startTime;
		this.expectedEndTime = expectedEndTime;
		this.currentMinute = currentMinute;
		this.totalDurationSeconds = totalDurationSeconds;
		this.secondsRemaining = secondsRemaining;
	}
	
	// Getters and Setters
	public boolean isRunning() {
		return isRunning;
	}
	
	public void setRunning(boolean running) {
		isRunning = running;
	}
	
	public LocalDateTime getStartTime() {
		return startTime;
	}
	
	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}
	
	public LocalDateTime getExpectedEndTime() {
		return expectedEndTime;
	}
	
	public void setExpectedEndTime(LocalDateTime expectedEndTime) {
		this.expectedEndTime = expectedEndTime;
	}
	
	public int getCurrentMinute() {
		return currentMinute;
	}
	
	public void setCurrentMinute(int currentMinute) {
		this.currentMinute = currentMinute;
	}
	
	public int getTotalDurationSeconds() {
		return totalDurationSeconds;
	}
	
	public void setTotalDurationSeconds(int totalDurationSeconds) {
		this.totalDurationSeconds = totalDurationSeconds;
	}
	
	public long getSecondsRemaining() {
		return secondsRemaining;
	}
	
	public void setSecondsRemaining(long secondsRemaining) {
		this.secondsRemaining = secondsRemaining;
	}
}
