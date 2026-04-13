package com.example.manager.config;

import com.example.manager.model.Team;
import com.example.manager.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * NOTE: This initializer is disabled.
 * Use POST /api/admin/generate-team to generate teams manually via API.
 */
// @Component
public class DummyDataInitializer implements CommandLineRunner {

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private com.example.manager.service.RepositoryService repositoryService;

	@Override
	public void run(String... args) throws Exception {
		// Disabled: Use API endpoint instead
		// ...existing commented code...
	}
}
