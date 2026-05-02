package com.example.manager.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.model.Message;
import com.example.manager.repository.MessageRepository;

/**
 * Service für Postfach/Inbox-Nachrichten
 */
@Service
public class MessageService {

	@Autowired
	private MessageRepository messageRepository;

	/**
	 * Erstellt eine neue Nachricht für ein Team
	 */
	@Transactional
	public Message createMessage(Long teamId, String title, String sender, String content, String type) {
		Message message = new Message(teamId, title, sender, content, type);
		return messageRepository.save(message);
	}

	/**
	 * Erstellt eine neue Nachricht mit ReferenceId
	 */
	@Transactional
	public Message createMessage(Long teamId, String title, String sender, String content, String type, Long referenceId) {
		Message message = new Message(teamId, title, sender, content, type);
		message.setReferenceId(referenceId);
		return messageRepository.save(message);
	}

	/**
	 * Findet alle Nachrichten für ein Team (chronologisch sortiert)
	 */
	public List<Message> getTeamMessages(Long teamId) {
		return messageRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
	}

	/**
	 * Findet ungelesene Nachrichten für ein Team
	 */
	public List<Message> getUnreadMessages(Long teamId) {
		return messageRepository.findByTeamIdAndReadFalseOrderByCreatedAtDesc(teamId);
	}

	/**
	 * Findet Nachrichten nach Typ
	 */
	public List<Message> getMessagesByType(Long teamId, String type) {
		return messageRepository.findByTeamIdAndTypeOrderByCreatedAtDesc(teamId, type);
	}

	/**
	 * Markiert eine Nachricht als gelesen
	 */
	@Transactional
	public Message markAsRead(Long messageId) {
		Message message = messageRepository.findById(messageId).orElse(null);
		if (message != null) {
			message.setRead(true);
			messageRepository.save(message);
		}
		return message;
	}

	/**
	 * Löscht eine Nachricht
	 */
	@Transactional
	public void deleteMessage(Long messageId) {
		messageRepository.deleteById(messageId);
	}

	/**
	 * Findet eine Nachricht nach ID
	 */
	public Message getMessage(Long messageId) {
		return messageRepository.findById(messageId).orElse(null);
	}

	/**
	 * Gibt Anzahl ungelesener Nachrichten zurück
	 */
	public int getUnreadCount(Long teamId) {
		return getUnreadMessages(teamId).size();
	}
}
