/**
 * Centralized theme color constants for the Send Node BST platform.
 * All view classes should reference these instead of defining their own.
 */
public final class Theme {

    private Theme() {} // utility class — no instantiation

    // ── Background Layers ──────────────────────────────────────────────────
    public static final String BG        = "#0f172a"; // deepest background (slate-900)
    public static final String SURFACE   = "#1e293b"; // card / panel surface (slate-800)
    public static final String NAV_BG    = "#111827"; // top nav bar (gray-900)
    public static final String SURFACE2  = "#334155"; // nested card surface (slate-700)

    // ── Brand Colors ───────────────────────────────────────────────────────
    public static final String PRIMARY   = "#2563eb"; // main blue CTA (blue-600)
    public static final String PRIMARY2  = "#1d4ed8"; // gradient end for blue (blue-700)
    public static final String ACCENT    = "#3b82f6"; // blue accent (blue-500)
    public static final String TEAL      = "#0ea5e9"; // visualizer accent (sky-500)

    // ── Semantic Colors ────────────────────────────────────────────────────
    public static final String SUCCESS   = "#10b981"; // green — passed / solved (emerald-500)
    public static final String WARN      = "#f59e0b"; // orange — medium difficulty (amber-500)
    public static final String DANGER    = "#ef4444"; // red — hard / error (red-500)

    // ── Text Colors ────────────────────────────────────────────────────────
    public static final String TEXT_MUTED  = "#64748b"; // slate-500
    public static final String TEXT_LIGHT  = "#cbd5e1"; // slate-300
    public static final String TEXT_WHITE  = "#ffffff";

    // ── Border ─────────────────────────────────────────────────────────────
    public static final String BORDER    = "#334155"; // slate-700
    public static final String BORDER2   = "#475569"; // slate-600

    // ── Difficulty → Color mapping ──────────────────────────────────────────
    public static String difficultyColor(String difficulty) {
        return switch (difficulty.toLowerCase()) {
            case "easy"   -> SUCCESS;
            case "medium" -> WARN;
            default       -> DANGER;
        };
    }
}
