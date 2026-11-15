package com.celestirift.api;

/**
 * Version of the public CelestiRift API. Bumped on breaking changes so dependent
 * plugins can check compatibility at runtime.
 */
public final class ApiVersion {

    /** Major API version; incremented on breaking changes. */
    public static final int MAJOR = 2;

    /** Minor API version; incremented on additive changes. */
    public static final int MINOR = 2;

    private ApiVersion() {}

    /**
     * Check compatibility with a plugin built against {@code major.minor}.
     *
     * @param major the API major version the caller was built against
     * @param minor the API minor version the caller was built against
     * @return true if this runtime API satisfies that version
     */
    public static boolean isCompatible(int major, int minor) {
        return major == MAJOR && minor <= MINOR;
    }
}
