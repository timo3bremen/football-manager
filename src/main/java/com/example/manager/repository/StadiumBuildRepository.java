package com.example.manager.repository;

import com.example.manager.model.StadiumBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StadiumBuildRepository extends JpaRepository<StadiumBuild, Long> {

    /**
     * Find active (not completed) builds for a team
     */
    List<StadiumBuild> findByTeamIdAndCompletedFalse(Long teamId);

    /**
     * Find the most recent active build for a team
     */
    Optional<StadiumBuild> findFirstByTeamIdAndCompletedFalseOrderByStartTimeDesc(Long teamId);

    /**
     * Find all completed builds for a team
     */
    List<StadiumBuild> findByTeamIdAndCompletedTrue(Long teamId);
}
