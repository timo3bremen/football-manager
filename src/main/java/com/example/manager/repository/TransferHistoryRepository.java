package com.example.manager.repository;

import com.example.manager.model.TransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {
    
    /**
     * Findet alle Transfer-Historie Einträge, sortiert nach Transfer-Zeit (neueste zuerst)
     */
    List<TransferHistory> findAllByOrderByTransferTimeDesc();
    
    /**
     * Findet Transfer-Historie für ein bestimmtes Team (als Käufer oder Verkäufer)
     */
    List<TransferHistory> findByToTeamIdOrFromTeamIdOrderByTransferTimeDesc(Long toTeamId, Long fromTeamId);
    
    /**
     * Findet Transfer-Historie für eine Saison
     */
    List<TransferHistory> findBySeasonOrderByTransferTimeDesc(Integer season);
    
    /**
     * Findet Transfer-Historie für einen Spieltag
     */
    List<TransferHistory> findBySeasonAndMatchdayOrderByTransferTimeDesc(Integer season, Integer matchday);
}
