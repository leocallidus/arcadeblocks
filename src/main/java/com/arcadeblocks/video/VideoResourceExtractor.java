package com.arcadeblocks.video;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extracts bundled video resources to a temp directory so backends that require file URIs can access them.
 */
public class VideoResourceExtractor {

    private static final VideoResourceExtractor INSTANCE = new VideoResourceExtractor();
    private final Path tempDir;

    private VideoResourceExtractor() {
        try {
            tempDir = Files.createTempDirectory("arcadeblocks_videos");
            tempDir.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temp directory for videos", e);
        }
    }

    public static VideoResourceExtractor getInstance() {
        return INSTANCE;
    }

    /**
     * Extract a resource from assets/textures to a temp file and return its absolute path.
     */
    public String extractVideo(String videoPath) throws IOException {
        String normalized = videoPath.startsWith("/") ? videoPath.substring(1) : videoPath;
        String resourcePath = "/assets/textures/" + normalized;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            File outFile = tempDir.resolve(new File(normalized).getName()).toFile();
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                is.transferTo(fos);
            }
            outFile.deleteOnExit();
            return outFile.getAbsolutePath();
        }
    }
}
