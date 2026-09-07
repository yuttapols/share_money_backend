package com.sharemoney.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryFileStorageService implements FileStorageService {

    private final Cloudinary cloudinary;

    @Override
    public StoredFile upload(MultipartFile file, String folder, boolean authenticated) {
        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "type", authenticated ? "authenticated" : "upload",
                    "resource_type", "auto");

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);

            return new StoredFile(
                    (String) result.get("public_id"),
                    (String) result.get("secure_url"),
                    file.getOriginalFilename(),
                    (String) result.get("format"),
                    (String) result.get("resource_type"),
                    ((Number) result.get("bytes")).longValue());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public String generateSignedUrl(String publicId, String resourceType) {
        return cloudinary.url()
                .resourceType(resourceType)
                .type("authenticated")
                .signed(true)
                .secure(true)
                .generate(publicId);
    }

    @Override
    public String generateSignedThumbnailUrl(String publicId, String resourceType, int width) {
        return cloudinary.url()
                .resourceType(resourceType)
                .type("authenticated")
                .signed(true)
                .secure(true)
                .transformation(new Transformation<>().width(width).crop("limit"))
                .generate(publicId);
    }
}
