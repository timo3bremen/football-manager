package com.example.manager.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Nachrichtenmodell für Postfach/Inbox
 * - Auslaufende Verträge
 * - Wettbewerbsprämien
 * - Verletzungen
 * - Sonstige Benachrichtigungen
 */
@Entity
@Table(name = "messages")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "teamId")
	private Long teamId;

	private String title;
	private String sender; // z.B. "System", "Ligaverwaltung", "Vereinsmanagement"
	
	@Column(columnDefinition = "TEXT")
	private String content;

	private String type; // "contract", "bonus", "injury", "info", etc.
	private LocalDateTime createdAt;
	private boolean read = false;

	// Für Referenzen (z.B. bei Vertrag: playerId, bei Bonus: leagueId)
	private Long referenceId;

	public Message() {
		this.createdAt = LocalDateTime.now();
	}

	public Message(Long teamId, String title, String sender, String content, String type) {
		this.teamId = teamId;
		this.title = title;
		this.sender = sender;
		this.content = content;
		this.type = type;
		this.createdAt = LocalDateTime.now();
	}

	// Getter und Setter
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public Long getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(Long referenceId) {
		this.referenceId = referenceId;
	}
}
