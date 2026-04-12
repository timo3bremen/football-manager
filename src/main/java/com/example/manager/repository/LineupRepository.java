package com.example.manager.repository;

import com.example.manager.model.LineupSlot;
import com.example.manager.model.LineupSlotId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineupRepository extends JpaRepository<LineupSlot, LineupSlotId> {
    
    List<LineupSlot> findByTeamIdAndFormationId(Long teamId, String formationId);
    
    List<LineupSlot> findByTeamId(Long teamId);
    
    void deleteByTeamIdAndFormationId(Long teamId, String formationId);
    
    void deleteByTeamId(Long teamId);
}
