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
  50%  { transform: translate(-53%, -55%) scale(1.1); opacity: .85; }
  100% { transform: translate(-50%, -50%) scale(1); opacity: .95; }
}

@keyframes heroFloat2 {
  0%   { transform: translate(0, 0) scale(1); opacity: .90; }
  50%  { transform: translate(40px, -30px) scale(1.12); opacity: .78; }
  100% { transform: translate(0, 0) scale(1); opacity: .90; }
}

@media (prefers-reduced-motion: reduce) {
  .hero-ambient-blob {
    animation-duration: 0.001ms !important;
    animation-iteration-count: 1 !important;
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

  // Only visibility changes on light (same motion/keyframes)
  const pink = isDark
      ? "radial-gradient(circle, rgba(226,68,98,0.22), transparent 70%)"
      : "radial-gradient(circle, rgba(226,68,98,0.26), transparent 68%)";

  const violet = isDark
      ? "radial-gradient(circle, rgba(127,82,255,0.20), transparent 70%)"
      : "radial-gradient(circle, rgba(127,82,255,0.24), transparent 68%)";

  // Optional: slightly faster on light (same keyframes, just cycles quicker)
  const dur1 = isDark ? "48s" : "40s";
  const dur2 = isDark ? "60s" : "50s";

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
            className="hero-ambient-blob"
            style={{
              position: "absolute",
              width: 520,
              height: 520,
              left: "50vw",
              top: "30vh",
              transform: "translate(-50%, -50%)",
              background: pink,
              filter: "blur(40px)",
              WebkitFilter: "blur(40px)",
              willChange: "transform, opacity",

              animationName: "heroFloat1",
              animationDuration: dur1,
              animationTimingFunction: "ease-in-out",
              animationIterationCount: "infinite",
            }}
        />

        {/* blob 2 */}
        <Stack
            className="hero-ambient-blob"
            style={{
              position: "absolute",
              width: 420,
              height: 420,
              left: "46vw",
              top: "38vh",
              background: violet,
              filter: "blur(50px)",
              WebkitFilter: "blur(50px)",
              willChange: "transform, opacity",

              animationName: "heroFloat2",
              animationDuration: dur2,
              animationTimingFunction: "ease-in-out",
              animationIterationCount: "infinite",
            }}
        />
      </Stack>
  );
}
