import { createFont } from "tamagui";

const sizes = {
  1: 11,
  2: 12,
  3: 13,
  4: 14,
  5: 16,
  6: 18,
  7: 20,
  8: 23,
  9: 30,
  10: 46,
  11: 55,
  12: 62,
  13: 72,
  14: 92,
  15: 114,
  16: 134,
} as const;

const lineHeights = Object.fromEntries(
    Object.entries(sizes).map(([k, v]) => [k, Number(v) + 6])
) as Record<keyof typeof sizes, number>;

export const fonts = {
  body: createFont({
    family: "var(--font-body)",
    size: sizes,
    lineHeight: lineHeights,
    weight: { 4: "400", 7: "700" },
  }),

  heading: createFont({
    family: "var(--font-body)",
    size: { ...sizes, 4: 18, 5: 22, 6: 26, 7: 30, 8: 36 },
    lineHeight: lineHeights,
    weight: { 4: "600", 7: "800" },
  }),

  mono: createFont({
    family:
        'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
    size: sizes,
    lineHeight: lineHeights,
    weight: { 4: "400", 7: "700" },
  }),
} as const;
