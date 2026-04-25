package com.example.manager.repository;

import com.example.manager.model.SimulationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SimulationMessageRepository extends JpaRepository<SimulationMessage, Long> {
    
    /**
     * Findet alle Nachrichten für ein bestimmtes Match, sortiert nach Erstellungszeit
     */
    List<SimulationMessage> findByMatchIdOrderByCreatedAtAsc(Long matchId);
    
    /**
     * Findet alle Nachrichten für ein bestimmtes Match und Team
     */
    List<SimulationMessage> findByMatchIdAndTeamIdOrderByCreatedAtAsc(Long matchId, Long teamId);
    
    /**
     * Löscht alle Nachrichten für ein bestimmtes Match
     */
    void deleteByMatchId(Long matchId);
    
    /**
     * Zählt Nachrichten für ein bestimmtes Match
     */
    long countByMatchId(Long matchId);
}
