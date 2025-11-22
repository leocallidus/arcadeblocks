package com.arcadeblocks.video;

/**
 * Factory for creating video player backends
 * Uses VLCJ exclusively - JavaFX Media support has been removed
 */
public class VideoBackendFactory {
    
    public enum BackendType {
        VLC_ONLY,  // Use VLC only, fail if unavailable
        STUB       // Use stub/no-op backend when VLC unavailable
    }
    
    private BackendType preferredBackend = BackendType.VLC_ONLY; // Try VLC first, fall back to STUB if unavailable
    private boolean vlcAvailable = false;
    
    public VideoBackendFactory() {
        checkVlcAvailability();
        // If VLC is not available, automatically switch to STUB
        if (!vlcAvailable) {
            preferredBackend = BackendType.STUB;
        }
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
        
        if (vlcAvailable) {
            // System.out.println("VLC backend available");
        } else {
            // System.out.println("VLC backend not available, will use JavaFX fallback");
            // System.out.println("  Reason: " + vlcContext.getErrorMessage());
        }
    }
    
    /**
     * Set preferred backend type
     */
    public void setPreferredBackend(BackendType backend) {
        this.preferredBackend = backend;
    }
    
    /**
     * Get preferred backend type
     */
    public BackendType getPreferredBackend() {
        return preferredBackend;
    }
    
    /**
     * Check if VLC is available
     */
    public boolean isVlcAvailable() {
        return vlcAvailable;
    }
    
    /**
     * Create video player backend based on configuration
     * @return VideoPlayerBackend instance (never throws exception, falls back to stub)
     */
    public VideoPlayerBackend createBackend() {
        try {
            switch (preferredBackend) {
                case VLC_ONLY:
                    if (!vlcAvailable) {
                        System.err.println("VLC backend requested but not available: " + 
                            VlcContext.getInstance().getErrorMessage());
                        // Fall back to stub instead of throwing exception
                        return new StubVideoBackend();
                    }
                    // Double-check VLC initialization before creating backend
                    VlcContext vlcContext = VlcContext.getInstance();
                    if (!vlcContext.isInitialized()) {
                        vlcAvailable = vlcContext.initialize();
                        if (!vlcAvailable) {
                            System.err.println("VLC backend initialization failed: " + vlcContext.getErrorMessage());
                            return new StubVideoBackend();
                        }
                    }
                    // System.out.println("Создание VLC backend...");
                    return new VlcjMediaBackend();
                    
                case STUB:
                    // Return a stub/no-op backend when VLC is unavailable
                    // System.out.println("Создание Stub backend...");
                    return new StubVideoBackend();
                    
                default:
                    if (vlcAvailable) {
                        // Double-check VLC initialization before creating backend
                        VlcContext vlcCtx = VlcContext.getInstance();
                        if (!vlcCtx.isInitialized()) {
                            vlcAvailable = vlcCtx.initialize();
                            if (!vlcAvailable) {
                                // System.out.println("VLC инициализация не удалась, создание Stub backend...");
                                return new StubVideoBackend();
                            }
                        }
                        // System.out.println("Создание VLC backend...");
                        return new VlcjMediaBackend();
                    } else {
                        // System.out.println("VLC недоступен, создание Stub backend...");
                        return new StubVideoBackend();
                    }
            }
        } catch (Exception e) {
            System.err.println("Error creating video backend: " + e.getMessage());
            // Always fall back to stub backend on any error
            return new StubVideoBackend();
        }
    }
    
    /**
     * Get backend info for debugging
     */
    public String getBackendInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Video Backend Configuration:\n");
        info.append("  Preferred: ").append(preferredBackend).append("\n");
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

