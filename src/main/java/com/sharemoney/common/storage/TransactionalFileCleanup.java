package com.sharemoney.common.storage;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class TransactionalFileCleanup {

    private static final Logger log = LoggerFactory.getLogger(TransactionalFileCleanup.class);

    private final FileStorageService fileStorageService;

    public void deleteAfterCommit(String publicId, String resourceType, boolean authenticated) {
        schedule(publicId, resourceType, authenticated, TransactionSynchronization.STATUS_COMMITTED, "replaced-file");
    }

    public void deleteOnRollback(String publicId, String resourceType, boolean authenticated) {
        schedule(publicId, resourceType, authenticated, TransactionSynchronization.STATUS_ROLLED_BACK, "compensating-upload");
    }

    private void schedule(String publicId, String resourceType, boolean authenticated, int wantedStatus, String reason) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeDelete(publicId, resourceType, authenticated, reason);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == wantedStatus) {
                    safeDelete(publicId, resourceType, authenticated, reason);
                }
            }
        });
    }

    private void safeDelete(String publicId, String resourceType, boolean authenticated, String reason) {
        try {
            fileStorageService.delete(publicId, resourceType, authenticated);
        } catch (RuntimeException e) {
            log.warn("orphan file left on storage ({}): publicId={} resourceType={} authenticated={} — reconcile manually",
                    reason, publicId, resourceType, authenticated, e);
        }
    }
}
