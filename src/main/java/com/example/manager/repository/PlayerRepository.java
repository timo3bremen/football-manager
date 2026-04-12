package com.example.manager.repository;

import com.example.manager.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    
    List<Player> findByTeamId(Long teamId);
    
    Player findByTeamIdAndName(Long teamId, String name);
    
    void deleteByTeamId(Long teamId);
}
