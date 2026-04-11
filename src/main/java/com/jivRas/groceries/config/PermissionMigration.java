package com.jivRas.groceries.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Legacy migration stub — kept for backwards compatibility but disabled.
 *
 * <p>The old endpoint+method based permission migration is no longer needed.
 * The system now uses @ModuleAction annotations + a module+action permission
 * matrix seeded by {@link DataSeeder}. Any incremental permission changes
 * should be made via the admin API at {@code PUT /api/role-permissions/matrix}.
 */
@Component
public class PermissionMigration implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("[PermissionMigration] Module+Action system active — no migration needed.");
    }
}
