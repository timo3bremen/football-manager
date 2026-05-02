package com.example.manager.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.manager.model.Message;
import com.example.manager.service.MessageService;

/**
 * API für Postfach/Inbox-Nachrichten
 */
@RestController
@RequestMapping("/api/v2/messages")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MessageController {

	@Autowired
	private MessageService messageService;

	/**
	 * GET /api/v2/messages/{teamId}
	 * Findet alle Nachrichten für ein Team (chronologisch sortiert)
	 */
	@GetMapping("/{teamId}")
	public ResponseEntity<List<Message>> getTeamMessages(@PathVariable Long teamId) {
		try {
			List<Message> messages = messageService.getTeamMessages(teamId);
			return ResponseEntity.ok(messages);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * GET /api/v2/messages/{teamId}/unread
	 * Findet ungelesene Nachrichten für ein Team
	 */
	@GetMapping("/{teamId}/unread")
	public ResponseEntity<List<Message>> getUnreadMessages(@PathVariable Long teamId) {
		try {
			List<Message> messages = messageService.getUnreadMessages(teamId);
			return ResponseEntity.ok(messages);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * GET /api/v2/messages/{teamId}/unread/count
	 * Gibt Anzahl ungelesener Nachrichten zurück
	 */
	@GetMapping("/{teamId}/unread/count")
	public ResponseEntity<Integer> getUnreadCount(@PathVariable Long teamId) {
		try {
			int count = messageService.getUnreadCount(teamId);
			return ResponseEntity.ok(count);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * GET /api/v2/messages/{teamId}/type/{type}
	 * Findet Nachrichten nach Typ
	 */
	@GetMapping("/{teamId}/type/{type}")
	public ResponseEntity<List<Message>> getMessagesByType(@PathVariable Long teamId, @PathVariable String type) {
		try {
			List<Message> messages = messageService.getMessagesByType(teamId, type);
			return ResponseEntity.ok(messages);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * GET /api/v2/messages/{id}
	 * Findet eine einzelne Nachricht und markiert sie als gelesen
	 */
	@GetMapping("/read/{id}")
	public ResponseEntity<Message> readMessage(@PathVariable Long id) {
		try {
			Message message = messageService.markAsRead(id);
			return ResponseEntity.ok(message);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * DELETE /api/v2/messages/{id}
	 * Löscht eine Nachricht
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteMessage(@PathVariable Long id) {
		try {
			messageService.deleteMessage(id);
			return ResponseEntity.ok("Nachricht gelöscht");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * PUT /api/v2/messages/{id}/mark-read
	 * Markiert eine Nachricht als gelesen
	 */
	@PutMapping("/{id}/mark-read")
	public ResponseEntity<Message> markRead(@PathVariable Long id) {
		try {
			Message message = messageService.markAsRead(id);
			return ResponseEntity.ok(message);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().build();
		}
	}
}
