import { Stack, useThemeName } from "tamagui";
import { useEffect } from "react";

const HERO_KEYFRAMES_ID = "hero-ambient-keyframes";

function ensureKeyframes() {
  if (typeof document === "undefined") return;
  if (document.getElementById(HERO_KEYFRAMES_ID)) return;

  const style = document.createElement("style");
  style.id = HERO_KEYFRAMES_ID;
  style.textContent = `
@keyframes heroFloat1 {
  0%   { transform: translate(-50%, -50%) scale(1); opacity: .95; }
  50%  { transform: translate(-48%, -46%) scale(1.08); opacity: .85; }
  100% { transform: translate(-50%, -50%) scale(1); opacity: .95; }
}

@keyframes heroFloat2 {
  0%   { transform: translate(0, 0) scale(1); opacity: .90; }
  50%  { transform: translate(40px, -30px) scale(1.12); opacity: .78; }
  100% { transform: translate(0, 0) scale(1); opacity: .90; }
}

/* optional: gentle rotation/scale drift for a 3rd layer if you add it later */
@keyframes heroFloat3 {
  0%   { transform: translate(-50%, -50%) rotate(0deg) scale(1); }
  50%  { transform: translate(-52%, -48%) rotate(6deg) scale(1.06); }
  100% { transform: translate(-50%, -50%) rotate(0deg) scale(1); }
}

/* respect reduced motion */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.001ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.001ms !important;
  }
}
`;
  document.head.appendChild(style);
}

export function HeroAmbient() {
  const themeName = useThemeName();
  const isDark = themeName?.startsWith("dark");

  useEffect(() => {
    ensureKeyframes();
  }, []);

  return (
      <Stack
          pointerEvents="none"
          position="absolute"
          inset={0}
          z={0}
          style={{ overflow: "hidden" }}
      >
        {/* blob 1 */}
        <Stack
            style={{
              position: "absolute",
              width: 520,
              height: 520,
              left: "50%",
              top: "30%",
              transform: "translate(-50%, -50%)",
              background: isDark
                  ? "radial-gradient(circle, rgba(226,68,98,0.22), transparent 70%)"
                  : "radial-gradient(circle, rgba(226,68,98,0.18), transparent 70%)",
              filter: "blur(40px)",
              willChange: "transform, opacity",
              animation: "heroFloat1 48s ease-in-out infinite",
            }}
        />

        {/* blob 2 */}
        <Stack
            style={{
              position: "absolute",
              width: 420,
              height: 420,
              left: "46%",
              top: "38%",
              background: isDark
                  ? "radial-gradient(circle, rgba(127,82,255,0.20), transparent 70%)"
                  : "radial-gradient(circle, rgba(127,82,255,0.16), transparent 70%)",
              filter: "blur(50px)",
              willChange: "transform, opacity",
              animation: "heroFloat2 60s ease-in-out infinite",
            }}
        />
      </Stack>
  );
}
