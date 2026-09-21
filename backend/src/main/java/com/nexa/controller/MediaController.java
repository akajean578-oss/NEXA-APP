package com.nexa.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Value("${nexa.upload.dir}")
    private String uploadDir;

    // Types et taille strictement limités : on ne fait jamais confiance à l'extension seule,
    // on vérifie le content-type détecté par le navigateur/le serveur.
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE_BYTES = 8L * 1024 * 1024; // 8 Mo

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@AuthenticationPrincipal UserDetails principal,
                                     @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Fichier vide.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            return ResponseEntity.badRequest().body("Fichier trop volumineux (8 Mo max).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body("Type de fichier non autorisé. Formats acceptés : jpg, png, webp, gif.");
        }

        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        // Nom de fichier régénéré côté serveur (jamais celui envoyé par le client)
        // pour éviter tout "path traversal" ou collision.
        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
        String filename = UUID.randomUUID() + extension;
        Path target = dir.resolve(filename).normalize();

        if (!target.getParent().equals(dir.toAbsolutePath().normalize()) && !target.startsWith(dir)) {
            return ResponseEntity.badRequest().body("Chemin de fichier invalide.");
        }

        file.transferTo(target);

        return ResponseEntity.ok(Map.of("url", "/uploads/" + filename));
    }
}
