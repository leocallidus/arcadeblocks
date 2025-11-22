package com.arcadeblocks.video;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Extracts video resources from JAR to temporary directory for VLC playback
 */
public class VideoResourceExtractor {
    
    private static final String VIDEO_RESOURCE_PATH = "/assets/textures/";
    private static VideoResourceExtractor instance;
    private final Path tempVideoDir;
    private final Map<String, File> extractedVideos = new HashMap<>();
    
    private VideoResourceExtractor() throws IOException {
        // Create temp directory for extracted videos
        tempVideoDir = Files.createTempDirectory("arcade_blocks_videos_");
        tempVideoDir.toFile().deleteOnExit();
        
        // Register shutdown hook to cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
    }
    
    public static synchronized VideoResourceExtractor getInstance() {
        if (instance == null) {
            try {
                instance = new VideoResourceExtractor();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create video extractor", e);
            }
        }
        return instance;
    }
    
    /**
     * Extract video from JAR to temp directory and return file path
     * @param videoPath relative path to video (e.g., "boss_intro.mp4")
     * @return absolute path to extracted video file
     */
    public String extractVideo(String videoPath) throws IOException {
        // Check if already extracted
        if (extractedVideos.containsKey(videoPath)) {
            File existing = extractedVideos.get(videoPath);
            if (existing.exists()) {
                return existing.getAbsolutePath();
            }
        }
        
        // Build resource path
        String resourcePath = VIDEO_RESOURCE_PATH + videoPath;
        
        // Get input stream from resources
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Video resource not found: " + resourcePath);
        }
        
        try {
            // Create temp file with same extension
            String fileName = new File(videoPath).getName();
            String extension = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = fileName.substring(dotIndex);
                fileName = fileName.substring(0, dotIndex);
            }
            
            Path tempFile = Files.createTempFile(tempVideoDir, fileName + "_", extension);
            tempFile.toFile().deleteOnExit();
            
            // Copy resource to temp file
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            File extractedFile = tempFile.toFile();
            extractedVideos.put(videoPath, extractedFile);
            
            System.out.println("✓ Extracted video: " + videoPath + " -> " + extractedFile.getAbsolutePath());
            
            return extractedFile.getAbsolutePath();
            
        } finally {
            inputStream.close();
        }
    }
    
    /**
     * Cleanup extracted videos
     */
    private void cleanup() {
        for (File file : extractedVideos.values()) {
            try {
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        
        try {
            if (tempVideoDir != null && Files.exists(tempVideoDir)) {
                Files.delete(tempVideoDir);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Get temp directory path
     */
    public Path getTempVideoDir() {
        return tempVideoDir;
    }
}
