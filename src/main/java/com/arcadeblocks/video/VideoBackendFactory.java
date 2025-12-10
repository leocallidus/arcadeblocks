package com.arcadeblocks.video;

/**
 * Factory for creating video player backends.
 * Tries VLCJ first, then falls back to JavaFX Media if VLC недоступен/ломается.
 */
public class VideoBackendFactory {
    
    private boolean vlcAvailable = false;
    
    public VideoBackendFactory() {
        checkVlcAvailability();
    }
    
    /**
     * Check if VLC is available
     */
    private void checkVlcAvailability() {
        VlcContext vlcContext = VlcContext.getInstance();
        if (!vlcContext.isInitialized()) {
            vlcAvailable = vlcContext.initialize();
        } else {
            vlcAvailable = true;
        }
    }
    
    public boolean isVlcAvailable() {
        return vlcAvailable;
    }
    
    /**
     * Create video player backend.
     * Priority: VLCJ → JavaFX Media. Throws if both fail.
     */
    public VideoPlayerBackend createBackend() {
        // Try VLC first if available
        if (vlcAvailable) {
            try {
                VlcContext vlcContext = VlcContext.getInstance();
                if (!vlcContext.isInitialized()) {
                    vlcAvailable = vlcContext.initialize();
                }
                if (vlcAvailable) {
                    return new VlcjMediaBackend();
                }
            } catch (Exception e) {
                System.err.println("VLC backend failed, falling back to JavaFX Media: " + e.getMessage());
            }
        }
        
        // Fallback: JavaFX Media
        try {
            return new JavaFxMediaBackend();
        } catch (Exception e) {
            System.err.println("JavaFX Media backend failed: " + e.getMessage());
            throw new IllegalStateException("No available video backend (VLC and JavaFX Media failed)", e);
        }
    }
    
    /**
     * Get backend info for debugging
     */
    public String getBackendInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Video Backend Configuration:\n");
        info.append("  VLC Available: ").append(vlcAvailable).append("\n");
        if (vlcAvailable) {
            VlcContext ctx = VlcContext.getInstance();
            if (ctx.getFactory() != null) {
                info.append("  VLC Version: ").append(ctx.getFactory().application().version()).append("\n");
            }
        } else {
            info.append("  VLC Error: ").append(VlcContext.getInstance().getErrorMessage()).append("\n");
        }
        return info.toString();
    }
}

