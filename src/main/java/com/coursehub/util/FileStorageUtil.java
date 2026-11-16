package com.coursehub.util;

import com.coursehub.exception.BadRequestException;
import com.coursehub.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Saves, reads and deletes uploaded files (videos and course materials).
 * The folders come from application.properties (@Value). Every file is stored under a generated
 * UUID name, so the client never decides the file name or the location on disk.
 * Only that stored name is saved in the database, never the full path.
 */
@Slf4j
@Component
public class FileStorageUtil {

    private final Path videoDir;
    private final Path materialDir;
    private final Set<String> videoExtensions;
    private final Set<String> materialExtensions;

    public FileStorageUtil(@Value("${app.storage.video-dir}") String videoDirectory,
                           @Value("${app.storage.material-dir}") String materialDirectory,
                           @Value("${app.storage.allowed-video-extensions}") Set<String> videoExtensions,
                           @Value("${app.storage.allowed-material-extensions}") Set<String> materialExtensions) {
        this.videoDir = Paths.get(videoDirectory).toAbsolutePath().normalize();
        this.materialDir = Paths.get(materialDirectory).toAbsolutePath().normalize();
        this.videoExtensions = videoExtensions;
        this.materialExtensions = materialExtensions;
    }

    // The save methods return the stored file name (UUID + extension).
    public String saveVideo(MultipartFile file) {
        return save(file, videoDir, videoExtensions);
    }

    public String saveMaterial(MultipartFile file) {
        return save(file, materialDir, materialExtensions);
    }

    public Resource loadVideo(String storedName) {
        return load(storedName, videoDir);
    }

    public Resource loadMaterial(String storedName) {
        return load(storedName, materialDir);
    }

    public void deleteVideo(String storedName) {
        delete(storedName, videoDir);
    }

    public void deleteMaterial(String storedName) {
        delete(storedName, materialDir);
    }

    private String save(MultipartFile file, Path directory, Set<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The uploaded file is empty");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
            throw new BadRequestException("Unsupported file type. Allowed types: " + allowedExtensions);
        }

        String storedName = UUID.randomUUID() + "." + extension.toLowerCase();
        try {
            Files.createDirectories(directory);
            file.transferTo(directory.resolve(storedName));
        } catch (IOException ex) {
            log.error("Could not store file {}", storedName, ex);
            throw new IllegalStateException("Could not store the file", ex);
        }
        return storedName;
    }

    private Resource load(String storedName, Path directory) {
        Path file = directory.resolve(storedName);
        if (!Files.isReadable(file)) {
            log.error("File is missing on disk: {}", file);
            throw new ResourceNotFoundException("File not found on the server");
        }
        return new FileSystemResource(file);
    }

    // A failed delete is only logged: the database row is already gone, so the file can be removed later by hand.
    private void delete(String storedName, Path directory) {
        try {
            Files.deleteIfExists(directory.resolve(storedName));
        } catch (IOException ex) {
            log.warn("Could not delete file {}. Delete it manually.", storedName, ex);
        }
    }
}
