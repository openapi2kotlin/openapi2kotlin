import type { ReactNode } from "react";
import { useMemo } from "react";
import { Stack, useTheme, useThemeName } from "tamagui";

function toCss(val: any, fallback: string) {
  const v = typeof val === "object" && val && "val" in val ? val.val : val;
  return typeof v === "string" ? v : fallback;
}

type Props = {
  children: ReactNode;
  /**
   * Scales the size/intensity of the ambient background.
   * - 1.0 = current
   * - 0.8 = smaller/tighter
   * - 0.65 = quite compact
   */
  scale?: number;
};

const px = (n: number, k: number) => `${Math.round(n * k)}px`;
const a = (n: number, k: number) => Math.max(0, Math.min(1, n * k));

export function AmbientBackground({ children, scale = 1 }: Props) {
  const theme = useTheme();
  const themeName = useThemeName();
  const isDark = themeName?.startsWith("dark");

  const styles = useMemo(() => {
    const background = toCss(theme.background, "#fff");

    // Make smaller => a touch lower opacity to keep it refined
    // (you can change 0.92 to 1.0 if you want same intensity)
    const alphaK = 0.92 / Math.max(0.6, scale);
    const darkEdge = "#1B1D1E";

    if (!isDark) {
      return {
        backgroundColor: background,
        backgroundImage: `
          radial-gradient(
            ${px(700, scale)} ${px(420, scale)} at 50% 32%,
            rgba(226, 68, 98, ${a(0.18, alphaK)}),
            transparent 70%
          ),
          radial-gradient(
            ${px(900, scale)} ${px(520, scale)} at 55% 36%,
            rgba(127, 82, 255, ${a(0.10, alphaK)}),
            transparent 75%
          ),
          radial-gradient(
            ${px(1600, scale)} ${px(1100, scale)} at 50% 45%,
            transparent 0%,
            rgba(255,255,255, ${a(0.85, 1)} ) 78%,
            rgba(255,255,255, ${a(0.95, 1)} ) 100%
          )
        `,
        backgroundRepeat: "no-repeat",
      } as const;
    }

    return {
      backgroundColor: background,
      backgroundImage: `
        radial-gradient(
          ${px(520, scale)} ${px(900, scale)} at 50% 42%,
          rgba(255, 180, 120, ${a(0.10, alphaK)}),
          transparent 70%
        ),
        radial-gradient(
          ${px(680, scale)} ${px(1100, scale)} at 52% 40%,
          rgba(255, 120, 180, ${a(0.07, alphaK)}),
          transparent 72%
        ),
        radial-gradient(
          ${px(900, scale)} ${px(900, scale)} at 35% 30%,
          rgba(120, 140, 255, ${a(0.05, alphaK)}),
          transparent 70%
        ),
        radial-gradient(
          ${px(1600, scale)} ${px(1100, scale)} at 50% 45%,
          transparent 0%,
          ${darkEdge} 78%,
          ${darkEdge} 100%
        )
      `,
      backgroundRepeat: "no-repeat",
      // Remove if you want it to scroll with content
      // backgroundAttachment: "fixed",
    } as const;
  }, [theme, isDark, scale]);

  return (
      <Stack minH="100vh" width="100%" position="relative">
        <Stack
            pointerEvents="none"
            style={{
              position: "absolute",
              inset: 0,
              minHeight: "100%",
              zIndex: 0,
              ...styles,
            }}
        />

        <Stack
            pointerEvents="none"
            style={{
              position: "absolute",
              inset: 0,
              zIndex: 0,
              opacity: isDark ? 0.10 : 0.06,
              mixBlendMode: isDark ? "soft-light" : "multiply",
              backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='220' height='220'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='220' height='220' filter='url(%23n)' opacity='.55'/%3E%3C/svg%3E")`,
            }}
        />

        <Stack position="relative" z={1}>
          {children}
        </Stack>
      </Stack>
  );
}
