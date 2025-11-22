package com.arcadeblocks.video;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for VLC warning dialog functionality
 */
public class VlcWarningTest {
    
    @Test
    public void testVideoBackendFactoryDefaultsToStub() {
        VideoBackendFactory factory = new VideoBackendFactory();
        assertEquals(VideoBackendFactory.BackendType.STUB, factory.getPreferredBackend(),
            "Factory should default to STUB backend to allow game to continue");
    }
    
    @Test
    public void testCreateBackendNeverThrowsException() {
        VideoBackendFactory factory = new VideoBackendFactory();
        
        // Should not throw exception even if VLC is not available
        assertDoesNotThrow(() -> {
            VideoPlayerBackend backend = factory.createBackend();
            assertNotNull(backend, "Backend should never be null");
        }, "createBackend() should never throw exception");
    }
    
    @Test
    public void testVlcContextInitializeWithoutWarning() {
        VlcContext context = VlcContext.getInstance();
        
        // Should not show dialog when showWarning is false
        assertDoesNotThrow(() -> {
            context.initialize(false);
        }, "initialize(false) should not throw exception");
    }
    
    @Test
    public void testBackendInfoDoesNotCrash() {
        VideoBackendFactory factory = new VideoBackendFactory();
        
        // Should return info string without crashing
        assertDoesNotThrow(() -> {
            String info = factory.getBackendInfo();
            assertNotNull(info, "Backend info should not be null");
            assertTrue(info.contains("Video Backend Configuration"), 
                "Info should contain configuration details");
        }, "getBackendInfo() should not throw exception");
    }
}
