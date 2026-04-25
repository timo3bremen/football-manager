package com.example.manager.repository;

import com.example.manager.model.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
    Optional<Sponsor> findByTeamId(Long teamId);
    void deleteByTeamId(Long teamId);
}
