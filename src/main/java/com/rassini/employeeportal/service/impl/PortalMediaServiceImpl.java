package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.config.PortalMediaProperties;
import com.rassini.employeeportal.dto.response.PortalMediaResponse;
import com.rassini.employeeportal.entity.PortalMediaEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.PortalContentMapper;
import com.rassini.employeeportal.repository.PortalMediaRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.PortalMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PortalMediaServiceImpl implements PortalMediaService {

    private static final Pattern SAFE_STORAGE_KEY = Pattern.compile("^[a-f0-9-]{36}\\.(jpg|jpeg|png|webp)$");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final PortalMediaProperties mediaProperties;
    private final PortalMediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final PortalContentMapper mapper;

    @Override
    @Transactional
    public PortalMediaResponse upload(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("El archivo es obligatorio y no puede estar vacío");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }

        if (bytes.length > mediaProperties.getMaxSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo excede el tamaño máximo permitido de 5 MB");
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String ext = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Extensión no permitida. Use JPG, PNG o WebP");
        }

        String detectedMime = detectMimeByMagicBytes(bytes);
        if (detectedMime == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El contenido del archivo no corresponde a una imagen JPG, PNG o WebP válida");
        }

        String declaredMime = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!matchesExtensionAndMime(ext, detectedMime, declaredMime)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El tipo MIME y la extensión del archivo no coinciden con su contenido real");
        }

        String storageKey = UUID.randomUUID() + "." + (ext.equals("jpeg") ? "jpg" : ext);
        Path baseDir = resolveBaseDir();
        Path target = resolveSafePath(baseDir, storageKey);

        try {
            Files.createDirectories(baseDir);
            Path tmp = Files.createTempFile(baseDir, "upload-", ".tmp");
            try {
                Files.write(tmp, bytes);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new BusinessException("Error al almacenar el archivo en disco");
        }

        UserEntity uploader = userId != null ? userRepository.findById(userId).orElse(null) : null;

        PortalMediaEntity saved = mediaRepository.save(PortalMediaEntity.builder()
                .storageKey(storageKey)
                .originalName(originalName)
                .contentType(detectedMime)
                .sizeBytes((long) bytes.length)
                .uploadedBy(uploader)
                .build());

        return mapper.toMediaResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResource load(Long mediaId) {
        PortalMediaEntity entity = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Medio", "id", mediaId));

        if (!SAFE_STORAGE_KEY.matcher(entity.getStorageKey()).matches()) {
            throw new BusinessException("Identificador de archivo inválido");
        }

        Path baseDir = resolveBaseDir();
        Path target = resolveSafePath(baseDir, entity.getStorageKey());
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ResourceNotFoundException("Archivo", "id", mediaId);
        }

        try {
            Resource resource = new UrlResource(target.toUri());
            return new MediaResource(entity, resource);
        } catch (MalformedURLException e) {
            throw new BusinessException("No se pudo resolver la ruta del archivo");
        }
    }

    private Path resolveBaseDir() {
        return Paths.get(mediaProperties.getPath()).toAbsolutePath().normalize();
    }

    private Path resolveSafePath(Path baseDir, String storageKey) {
        Path resolved = baseDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new BusinessException("Ruta de archivo no permitida");
        }
        return resolved;
    }

    private String sanitizeOriginalName(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BusinessException("El nombre del archivo es obligatorio");
        }
        String name = Paths.get(filename).getFileName().toString().trim();
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String detectMimeByMagicBytes(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private boolean matchesExtensionAndMime(String ext, String detectedMime, String declaredMime) {
        boolean extMatches = switch (detectedMime) {
            case "image/jpeg" -> ext.equals("jpg") || ext.equals("jpeg");
            case "image/png" -> ext.equals("png");
            case "image/webp" -> ext.equals("webp");
            default -> false;
        };
        if (!extMatches) {
            return false;
        }
        if (declaredMime.isBlank() || declaredMime.equals("application/octet-stream")) {
            return true;
        }
        return declaredMime.equals(detectedMime) || (detectedMime.equals("image/jpeg") && declaredMime.equals("image/jpg"));
    }
}
