package com.vichovong.restaurant_pos.feature.upload.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private static final String UPLOAD_DIR = "./uploads/";

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            Path cardPath = uploadPath.resolve("card");
            if (!Files.exists(cardPath)) Files.createDirectories(cardPath);
            Path thumbPath = uploadPath.resolve("thumb");
            if (!Files.exists(thumbPath)) Files.createDirectories(thumbPath);

            // Read original image
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                return ResponseEntity.badRequest().build();
            }

            // Save hero (max 800px)
            saveResizedImage(originalImage, uploadPath.resolve(filename).toFile(), 800, extension);

            // Save card (max 512px)
            saveResizedImage(originalImage, cardPath.resolve(filename).toFile(), 512, extension);

            // Save thumb (max 96px)
            saveResizedImage(originalImage, thumbPath.resolve(filename).toFile(), 96, extension);

            String fileUrl = "/uploads/" + filename;
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private void saveResizedImage(BufferedImage originalImage, File outputFile, int targetWidth, String extension) throws IOException {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width > targetWidth) {
            double ratio = (double) targetWidth / width;
            width = targetWidth;
            height = (int) (height * ratio);
        }

        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();

        String formatName = extension.replace(".", "");
        if (formatName.isEmpty()) formatName = "jpg";
        if (formatName.equalsIgnoreCase("png")) {
             resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
             g = resizedImage.createGraphics();
             g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
             g.drawImage(originalImage, 0, 0, width, height, null);
             g.dispose();
        }

        ImageIO.write(resizedImage, formatName, outputFile);
    }
}
