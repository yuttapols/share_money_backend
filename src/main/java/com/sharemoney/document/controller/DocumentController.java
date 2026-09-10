package com.sharemoney.document.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.document.dto.DocumentResponse;
import com.sharemoney.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR','DEBTOR')")
    public ApiResponse<List<DocumentResponse>> list() {
        return ApiResponse.success(documentService.list());
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR','DEBTOR')")
    public ApiResponse<DocumentResponse> download(@PathVariable Long id) {
        return ApiResponse.success(documentService.getDownloadInfo(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR')")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(@RequestPart String title,
                                                                  @RequestPart(required = false) String debtorUsername,
                                                                  @RequestPart MultipartFile file) {
        DocumentResponse response = documentService.upload(title, debtorUsername, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document has been uploaded successfully."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CREDITOR')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.success(null, "Document has been deleted successfully.");
    }
}
