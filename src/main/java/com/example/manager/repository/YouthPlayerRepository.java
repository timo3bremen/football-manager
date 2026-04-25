package com.example.manager.repository;

import com.example.manager.model.YouthPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface YouthPlayerRepository extends JpaRepository<YouthPlayer, Long> {
    List<YouthPlayer> findByTeamId(Long teamId);
    List<YouthPlayer> findByTeamIdAndIsInAcademyTrue(Long teamId);
    List<YouthPlayer> findByScoutId(Long scoutId);
}
