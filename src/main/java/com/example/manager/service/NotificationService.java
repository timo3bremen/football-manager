package com.example.manager.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.repository.MessageRepository;
import com.example.manager.repository.PlayerRepository;
import com.example.manager.repository.TeamRepository;

/**
 * Service für automatisches Erstellen von Benachrichtigungen
 */
@Service
public class NotificationService {

	@Autowired
	private MessageService messageService;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private MessageRepository messageRepository;

	/**
	 * Erstellt Benachrichtigungen für auslaufende Verträge (User-Teams nur) Werden
	 * aufgerufen, wenn noch 2 oder 3 Saisons Restlaufzeit sind
	 */
	@Transactional
	public void notifyExpiringContracts() {
		System.out.println("[Notifications] 📋 Prüfe auf auslaufende Verträge...");

		List<Team> userTeams = teamRepository.findAll().stream().filter(t -> !t.isCPU()).toList();

		int totalNotifications = 0;

		for (Team team : userTeams) {
			List<Player> players = playerRepository.findByTeamId(team.getId());

			for (Player player : players) {
				// Notifiziere bei 3 oder 2 Saisons Restlaufzeit
				if (player.getContractLength() == 3 || player.getContractLength() == 2) {
					String title = "Vertrag läuft bald aus: " + player.getName();
					String content = String.format(
							"Der Vertrag von %s läuft in %d Saison(en) aus.\n\n" + "Aktuelles Gehalt: %d pro Saison\n"
									+ "Position: %s\n\n"
									+ "Du solltest den Vertrag bald verlängern, um den Spieler nicht zu verlieren.",
							player.getName(), player.getContractLength(), player.getSalary(),
							player.getPosition() != null ? player.getPosition() : "Unbekannt");

					messageService.createMessage(team.getId(), title, "Vereinsmanagement", content, "contract",
							player.getId());
					totalNotifications++;
					System.out.println("[Notifications] ✅ Benachrichtigung erstellt: " + player.getName() + " ("
							+ team.getName() + ")");
				}
			}
		}

		System.out.println("[Notifications] 📊 " + totalNotifications + " Benachrichtigungen erstellt");
	}

	/**
	 * Erstellt eine Benachrichtigung für Wettbewerbsprämien
	 */
	@Transactional
	public void notifyBonuses(Long teamId, String leagueName, int position, long bonusAmount) {
		String title = "💰 Wettbewerbsprämie erhalten!";
		String content = String.format("Glückwunsch! Du hast eine Wettbewerbsprämie erhalten.\n\n" + "Liga: %s\n"
				+ "Platzierung: %d\n" + "Prämie: %d €", leagueName, position, bonusAmount);

		messageService.createMessage(teamId, title, "Ligaverwaltung", content, "bonus");
		System.out.println("[Notifications] ✅ Bonus-Benachrichtigung erstellt");
	}

	/**
	 * Erstellt eine Benachrichtigung für Verletzungen (nur einmal pro Spieler und
	 * Verletzung)
	 */
	@Transactional
	public void notifyInjury(Long teamId, String playerName, int gamesOut, String diagnosis) {
		// Finde den Spieler
		List<Player> teamPlayers = playerRepository.findByTeamId(teamId);
		Player player = teamPlayers.stream().filter(p -> playerName.equals(p.getName())).findFirst().orElse(null);

		if (player == null) {
			System.out.println("[Notifications] ⚠️ Spieler nicht gefunden: " + playerName);
			return;
		}

		// Prüfe ob bereits eine Verletzungs-Benachrichtigung für diesen Spieler
		// existiert
		if (messageRepository.existsByTeamIdAndTypeAndReferenceId(teamId, "injury", player.getId()) && gamesOut > 0) {
			System.out.println("[Notifications] ℹ️ Verletzungs-Benachrichtigung existiert bereits für: " + playerName);
			return;
		}

		if (gamesOut <= 0) {
			String title = "🤕 " + playerName + " ist wieder fit!";
			String content = String.format(
					"Gute Nachrichten! %s ist wieder gesund und kann zur Aufstellung hinzugefügt werden.", playerName);
			messageService.createMessage(teamId, title, "Vereinsmanagement", content, "injury", player.getId());
		} else {
			String title = "🤕 " + playerName + " ist verletzt";
			String content = String.format(
					"Spieler: %s\n" + "Diagnose: %s\n" + "Ausfallszeit: %d Spieltag(e)\n\n"
							+ "Der Spieler kann nicht zur Aufstellung hinzugefügt werden, bis er sich geheilt hat.",
					playerName, diagnosis, gamesOut);

			messageService.createMessage(teamId, title, "Vereinsmanagement", content, "injury", player.getId());
		}
		System.out.println("[Notifications] ✅ Verletzungs-Benachrichtigung erstellt für: " + playerName);
	}

	/**
	 * Erstellt eine generische Informationsnachricht
	 */
	@Transactional
	public void notifyInfo(Long teamId, String title, String content) {
		messageService.createMessage(teamId, title, "System", content, "info");
		System.out.println("[Notifications] ✅ Info-Benachrichtigung erstellt");
	}

	/**
	 * Erstellt eine Benachrichtigung für Sperrung (Rote Karte) (nur einmal pro
	 * Spieler und Sperrung)
	 */
	@Transactional
	public void notifySuspension(Long teamId, String playerName, int matchesOut) {
		// Finde den Spieler
		List<Player> teamPlayers = playerRepository.findByTeamId(teamId);
		Player player = teamPlayers.stream().filter(p -> playerName.equals(p.getName())).findFirst().orElse(null);

		if (player == null) {
			System.out.println("[Notifications] ⚠️ Spieler nicht gefunden: " + playerName);
			return;
		}

		// Prüfe ob bereits eine Sperrung-Benachrichtigung für diesen Spieler existiert
		if (messageRepository.existsByTeamIdAndTypeAndReferenceId(teamId, "suspension", player.getId())
				&& matchesOut > 0) {
			System.out.println("[Notifications] ℹ️ Sperrung-Benachrichtigung existiert bereits für: " + playerName);
			return;
		}

		if (matchesOut >= 0) {
			String title = "🟥 Rote Karte: " + playerName;
			String content = String.format("Die Sperre von %s ist abgelaufen!", playerName);
			messageService.createMessage(teamId, title, "Schiedsrichter", content, "suspension", player.getId());
		} else {

			String title = "🟥 Rote Karte: " + playerName;
			String content = String.format(
					"%s hat eine rote Karte erhalten!\n\n" + "Sperrung: %d Spiel(e)\n\n"
							+ "Der Spieler kann in den nächsten %d Spielen nicht eingesetzt werden.",
					playerName, matchesOut, matchesOut);

			messageService.createMessage(teamId, title, "Schiedsrichter", content, "suspension", player.getId());
		}
		System.out.println("[Notifications] ✅ Sperrung-Benachrichtigung erstellt für: " + playerName);
	}
}
