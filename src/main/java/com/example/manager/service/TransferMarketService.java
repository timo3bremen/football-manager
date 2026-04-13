package com.example.manager.service;

import com.example.manager.model.Player;
import com.example.manager.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service für Transfermarkt-Operationen
 */
@Service
public class TransferMarketService {

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Gibt alle Spieler auf dem Transfermarkt zurück (Spieler ohne Team)
     */
    public List<Player> getAvailablePlayers() {
        return playerRepository.findAll().stream()
                .filter(p -> p.getTeamId() == null || p.getTeamId() == 0)
                .collect(Collectors.toList());
    }

    /**
     * Listet Spieler eines Teams zum Verkauf an
     */
    public Player listPlayerForSale(Long playerId, Long teamId) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player != null && player.getTeamId() != null && player.getTeamId().equals(teamId)) {
            // Spieler wird zum Verkauf angeboten, indem teamId auf null gesetzt wird
            player.setTeamId(null);
            return playerRepository.save(player);
        }
        return null;
    }

    /**
     * Kauft einen Spieler für ein Team
     */
    public Player buyPlayer(Long playerId, Long teamId) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player != null && (player.getTeamId() == null || player.getTeamId() == 0)) {
            player.setTeamId(teamId);
            return playerRepository.save(player);
        }
        return null;
    }

    /**
     * Sucht nach Spielern basierend auf Kriterien
     */
    public List<Player> searchPlayers(String position, Integer minRating, Integer maxRating) {
        return getAvailablePlayers().stream()
                .filter(p -> position == null || position.isEmpty() || position.equals(p.getPosition()))
                .filter(p -> minRating == null || p.getRating() >= minRating)
                .filter(p -> maxRating == null || p.getRating() <= maxRating)
                .collect(Collectors.toList());
    }
}
