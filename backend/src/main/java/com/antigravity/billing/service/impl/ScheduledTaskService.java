package com.antigravity.billing.service.impl;

import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Scheduled background jobs for routine system maintenance and alerts.
 * All tasks log to the application log and write system audit entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final ProductRepository productRepository;
    private final AuditService auditService;

    /**
     * Runs every day at 8:00 AM.
     * Checks for low-stock products and logs a system alert for each.
     * Future: Can send email notifications when an email service is configured.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void checkLowStockAlerts() {
        log.info("[Scheduled] Running daily low-stock check at {}", Instant.now());
        try {
            var lowStockPage = productRepository.findLowStockProducts(PageRequest.of(0, 100));
            if (lowStockPage.isEmpty()) {
                log.info("[Scheduled] Low-stock check: All products are sufficiently stocked.");
                return;
            }

            log.warn("[Scheduled] Low-stock alert: {} product(s) need restocking:", lowStockPage.getTotalElements());
            lowStockPage.getContent().forEach(p -> {
                log.warn("  -> [{}] {} -- Current stock: {}, Min required: {}", p.getSku(), p.getName(), p.getCurrentStock(), p.getMinStockLevel());
                auditService.logAction(
                        null, "SYSTEM",
                        "LOW_STOCK_ALERT",
                        "PRODUCT",
                        p.getId().toString(),
                        String.format("Product '%s' (SKU: %s) has low stock: %d (min: %d)",
                                p.getName(), p.getSku(), p.getCurrentStock(), p.getMinStockLevel())
                );
            });
        } catch (Exception ex) {
            log.error("[Scheduled] Low-stock check failed", ex);
        }
    }


    /**
     * Runs every Sunday at 2:00 AM.
     * Logs a system health heartbeat confirming all services are operational.
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void weeklySystemHealthCheck() {
        log.info("[Scheduled] Weekly system health check at {}", Instant.now());
        try {
            long productCount = productRepository.count();
            log.info("[Scheduled] Health check OK — Products in catalog: {}", productCount);

            auditService.logAction(
                    null, "SYSTEM",
                    "WEEKLY_HEALTH_CHECK",
                    "SYSTEM",
                    UUID.randomUUID().toString(),
                    String.format("Weekly health check passed. Products: %d", productCount)
            );
        } catch (Exception ex) {
            log.error("[Scheduled] Weekly health check failed", ex);
        }
    }

    /**
     * Runs on the 1st of every month at 6:00 AM.
     * Creates a monthly maintenance audit record (placeholder for future backup trigger).
     */
    @Scheduled(cron = "0 0 6 1 * *")
    public void monthlyMaintenanceRecord() {
        log.info("[Scheduled] Monthly maintenance record at {}", Instant.now());
        try {
            auditService.logAction(
                    null, "SYSTEM",
                    "MONTHLY_MAINTENANCE",
                    "SYSTEM",
                    UUID.randomUUID().toString(),
                    "Monthly maintenance window: Verify DB backups, review audit logs, check certificate expiry."
            );
            log.info("[Scheduled] Monthly maintenance record created.");
        } catch (Exception ex) {
            log.error("[Scheduled] Monthly maintenance record failed", ex);
        }
    }
}
