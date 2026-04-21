package com.pscholer.autoplayer.util;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\rB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J:\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\u0006\u00a8\u0006\u000e"}, d2 = {"Lcom/pscholer/autoplayer/util/AspectRatioCalculator;", "", "()V", "calculateScaling", "Lcom/pscholer/autoplayer/util/AspectRatioCalculator$ScaledDimensions;", "videoWidth", "", "videoHeight", "pixelAspectRatio", "", "rotationDegrees", "containerWidth", "containerHeight", "ScaledDimensions", "app_debug"})
public final class AspectRatioCalculator {
    @org.jetbrains.annotations.NotNull()
    public static final com.pscholer.autoplayer.util.AspectRatioCalculator INSTANCE = null;
    
    private AspectRatioCalculator() {
        super();
    }
    
    /**
     * Calculate how to scale video to fit container while preserving aspect ratio.
     *
     * Returns target dimensions and center offsets for letterboxing/pillarboxing.
     * If video is wider than container, it fills width with padding on top/bottom.
     * If video is taller than container, it fills height with padding on left/right.
     */
    @org.jetbrains.annotations.NotNull()
    public final com.pscholer.autoplayer.util.AspectRatioCalculator.ScaledDimensions calculateScaling(int videoWidth, int videoHeight, float pixelAspectRatio, int rotationDegrees, int containerWidth, int containerHeight) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J1\u0010\u0011\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0015\u001a\u00020\u0016H\u00d6\u0001J\t\u0010\u0017\u001a\u00020\u0018H\u00d6\u0001R\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\t\u00a8\u0006\u0019"}, d2 = {"Lcom/pscholer/autoplayer/util/AspectRatioCalculator$ScaledDimensions;", "", "targetWidth", "", "targetHeight", "offsetX", "offsetY", "(FFFF)V", "getOffsetX", "()F", "getOffsetY", "getTargetHeight", "getTargetWidth", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
    public static final class ScaledDimensions {
        private final float targetWidth = 0.0F;
        private final float targetHeight = 0.0F;
        private final float offsetX = 0.0F;
        private final float offsetY = 0.0F;
        
        public ScaledDimensions(float targetWidth, float targetHeight, float offsetX, float offsetY) {
            super();
        }
        
        public final float getTargetWidth() {
            return 0.0F;
        }
        
        public final float getTargetHeight() {
            return 0.0F;
        }
        
        public final float getOffsetX() {
            return 0.0F;
        }
        
        public final float getOffsetY() {
            return 0.0F;
        }
        
        public final float component1() {
            return 0.0F;
        }
        
        public final float component2() {
            return 0.0F;
        }
        
        public final float component3() {
            return 0.0F;
        }
        
        public final float component4() {
            return 0.0F;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pscholer.autoplayer.util.AspectRatioCalculator.ScaledDimensions copy(float targetWidth, float targetHeight, float offsetX, float offsetY) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}