package g_vael.cmsall.core;

/** How a player triggers the chain. */
public enum ActivationMode {
    /** Active only while sneaking (default). */
    HOLD,
    /** Active while a persistent on/off toggle is on (keybind or /cmsall toggle). */
    TOGGLE,
    /** Always active. */
    ALWAYS,
    /** Active except while sneaking (FallingTree-style invert). */
    SNEAK_INVERT;

    public String id() {
        return name().toLowerCase();
    }

    public static ActivationMode parse(String s, ActivationMode fallback) {
        if (s == null) {
            return fallback;
        }
        return switch (s.toLowerCase()) {
            case "hold" -> HOLD;
            case "toggle" -> TOGGLE;
            case "always" -> ALWAYS;
            case "sneak_invert", "sneak-invert" -> SNEAK_INVERT;
            default -> fallback;
        };
    }
}
