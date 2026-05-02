package com.example.manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.manager.model.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

	List<Player> findByTeamId(Long teamId);

	List<Player> findByIsInjured(boolean isInjured);

	List<Player> findByIsSuspended(boolean isSuspended);

	Player findByTeamIdAndName(Long teamId, String name);

	void deleteByTeamId(Long teamId);
}
