package gg.azura.bridges.gui;

/**
 * Manages sound settings for player interactions
 *
 * @author SyncFocus17
 * @created 2025-02-04 11:08:49 UTC
 */
public class SoundSettings {
    private float volume;
    private float pitch;
    private boolean echoEffect;
    private final long createdAt;

    /**
     * Creates sound settings with default values
     */
    public SoundSettings() {
        this(1.0f, 1.0f, false);
    }

    /**
     * Creates sound settings with custom values
     *
     * @param volume Initial volume (0.0 to 2.0)
     * @param pitch Initial pitch (0.5 to 2.0)
     * @param echoEffect Whether echo effect is enabled
     */
    public SoundSettings(float volume, float pitch, boolean echoEffect) {
        this.volume = validateVolume(volume);
        this.pitch = validatePitch(pitch);
        this.echoEffect = echoEffect;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Gets the current volume
     *
     * @return Volume value between 0.0 and 2.0
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Sets a new volume value
     *
     * @param volume New volume (0.0 to 2.0)
     */
    public void setVolume(float volume) {
        this.volume = validateVolume(volume);
    }

    /**
     * Gets the current pitch
     *
     * @return Pitch value between 0.5 and 2.0
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Sets a new pitch value
     *
     * @param pitch New pitch (0.5 to 2.0)
     */
    public void setPitch(float pitch) {
        this.pitch = validatePitch(pitch);
    }

    /**
     * Checks if echo effect is enabled
     *
     * @return True if echo effect is on
     */
    public boolean hasEchoEffect() {
        return echoEffect;
    }

    /**
     * Sets echo effect status
     *
     * @param echoEffect New echo effect status
     */
    public void setEchoEffect(boolean echoEffect) {
        this.echoEffect = echoEffect;
    }

    /**
     * Gets creation timestamp
     *
     * @return Creation time in milliseconds
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Validates and clamps volume value
     *
     * @param volume Volume to validate
     * @return Clamped volume between 0.0 and 2.0
     */
    private float validateVolume(float volume) {
        return Math.min(Math.max(volume, 0.0f), 2.0f);
    }

    /**
     * Validates and clamps pitch value
     *
     * @param pitch Pitch to validate
     * @return Clamped pitch between 0.5 and 2.0
     */
    private float validatePitch(float pitch) {
        return Math.min(Math.max(pitch, 0.5f), 2.0f);
    }

    /**
     * Creates a copy of these settings
     *
     * @return New SoundSettings instance with same values
     */
    public SoundSettings copy() {
        return new SoundSettings(volume, pitch, echoEffect);
    }

    /**
     * Resets settings to defaults
     */
    public void reset() {
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.echoEffect = false;
    }

    @Override
    public String toString() {
        return String.format(
                "SoundSettings[volume=%.1f, pitch=%.1f, echo=%b, created=%d]",
                volume,
                pitch,
                echoEffect,
                createdAt
        );
    }
}