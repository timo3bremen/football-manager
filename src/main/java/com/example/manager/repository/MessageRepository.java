package com.example.manager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.manager.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
	
	/**
	 * Findet alle Nachrichten für ein Team, sortiert nach Erstellungszeit (neueste zuerst)
	 */
	List<Message> findByTeamIdOrderByCreatedAtDesc(Long teamId);
	
	/**
	 * Findet ungelesene Nachrichten für ein Team
	 */
	List<Message> findByTeamIdAndReadFalseOrderByCreatedAtDesc(Long teamId);
	
	/**
	 * Findet Nachrichten nach Typ
	 */
	List<Message> findByTeamIdAndTypeOrderByCreatedAtDesc(Long teamId, String type);
	
	/**
	 * Findet Nachrichten nach Typ und ReferenceId (z.B. für einen bestimmten Spieler)
	 */
	List<Message> findByTeamIdAndTypeAndReferenceId(Long teamId, String type, Long referenceId);
	
	/**
	 * Prüft ob eine Nachricht mit bestimmtem Typ und ReferenceId existiert
	 */
	boolean existsByTeamIdAndTypeAndReferenceId(Long teamId, String type, Long referenceId);
	
	/**
	 * Löscht alle Nachrichten für ein Team
	 */
	void deleteByTeamId(Long teamId);
}
