package com.sharemoney.document.service;

import com.sharemoney.common.audit.AuditActions;
import com.sharemoney.common.audit.AuditLogService;
import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.common.storage.FileStorageService;
import com.sharemoney.common.storage.FileValidator;
import com.sharemoney.common.storage.StoredFile;
import com.sharemoney.common.storage.TransactionalFileCleanup;
import com.sharemoney.document.dto.DocumentResponse;
import com.sharemoney.document.entity.Document;
import com.sharemoney.document.repository.DocumentRepository;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;
    private static final String FOLDER = "sharemoney/documents";

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final TransactionalFileCleanup fileCleanup;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<DocumentResponse> list() {
        List<Document> documents = SecurityUtils.isAdmin()
                ? documentRepository.findAllWithOwners()
                : documentRepository.findVisibleToCreditor(SecurityUtils.currentUserId());
        return documents.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDownloadInfo(Long documentId) {
        Document document = getViewable(documentId);
        return toResponse(document);
    }

    @Transactional
    public DocumentResponse upload(String title, String debtorUsername, MultipartFile file) {
        FileValidator.assertDocument(file, MAX_DOCUMENT_BYTES);

        User uploader = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User debtor = null;
        if (StringUtils.hasText(debtorUsername)) {
            debtor = userRepository.findByUsernameIgnoreCase(debtorUsername)
                    .filter(user -> user.getRole() == UserRole.DEBTOR)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            boolean ownedByCreditor = debtor.getCreditor() != null && debtor.getCreditor().getId().equals(uploader.getId());
            if (!SecurityUtils.isAdmin() && !ownedByCreditor) {
                throw new BusinessException(ErrorCode.DEBTOR_NOT_OWNED);
            }
        }

        StoredFile stored = fileStorageService.upload(file, FOLDER, true);
        fileCleanup.deleteOnRollback(stored.publicId(), stored.resourceType(), true);

        Document document = new Document();
        document.setOwnerCreditor(SecurityUtils.isAdmin() ? null : uploader);
        document.setDebtor(debtor);
        document.setTitle(title);
        document.setPublicId(stored.publicId());
        document.setSecureUrl(stored.secureUrl());
        document.setOriginalFilename(stored.originalFilename());
        document.setFormat(stored.format());
        document.setResourceType(stored.resourceType());
        document.setBytes(stored.bytes());
        document.setUploadedBy(uploader);
        documentRepository.save(document);

        auditLogService.record(uploader.getUsername(), SecurityUtils.currentRole(),
                AuditActions.UPLOAD_DOCUMENT, null);

        return toResponse(document);
    }

    @Transactional
    public void delete(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        boolean ownedByCreditor = document.getOwnerCreditor() != null
                && document.getOwnerCreditor().getId().equals(SecurityUtils.currentUserId());
        if (!SecurityUtils.isAdmin() && !ownedByCreditor) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_OWNED);
        }

        String publicId = document.getPublicId();
        String resourceType = document.getResourceType();

        documentRepository.delete(document);
        fileCleanup.deleteAfterCommit(publicId, resourceType, true);

        auditLogService.record(SecurityUtils.currentUsername(), SecurityUtils.currentRole(),
                AuditActions.DELETE_DOCUMENT, null);
    }

    private Document getViewable(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        boolean isTemplate = document.getOwnerCreditor() == null;
        boolean ownedByCreditor = document.getOwnerCreditor() != null
                && document.getOwnerCreditor().getId().equals(SecurityUtils.currentUserId());

        if (!SecurityUtils.isAdmin() && !isTemplate && !ownedByCreditor) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_OWNED);
        }
        return document;
    }

    private DocumentResponse toResponse(Document document) {
        String scope = document.getDebtor() != null ? "DEBTOR" : "TEMPLATE";
        String debtorUsername = document.getDebtor() != null ? document.getDebtor().getUsername() : null;
        String url = fileStorageService.generateSignedUrl(document.getPublicId(), document.getResourceType());
        return new DocumentResponse(document.getId(), document.getTitle(), scope, debtorUsername, url);
    }
}
