package G_Vael.cmsall.core;

/** How a player triggers the chain. */
public enum ActivationMode {
    HOLD,
    TOGGLE,
    ALWAYS,
    SNEAK_INVERT;

    public String id() {
        return name().toLowerCase();
    }

    public static ActivationMode parse(String s, ActivationMode fallback) {
        if (s == null) {
            return fallback;
        }
        switch (s.toLowerCase()) {
            case "hold":
                return HOLD;
            case "toggle":
                return TOGGLE;
            case "always":
                return ALWAYS;
            case "sneak_invert":
            case "sneak-invert":
                return SNEAK_INVERT;
            default:
                return fallback;
        }
    }
}
