import { createFont } from 'tamagui'

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
}

export const fonts = {
  body: createFont({
    family: 'var(--font-body)',          // 👈 reuse what;s defined by next/font
    size: sizes,
    lineHeight: Object.fromEntries(
        Object.entries(sizes).map(([k, v]) => [k, Number(v) + 6])
    ),
    weight: {
      4: '400',
      7: '700',
    },
  }),

  heading: createFont({
    family: 'var(--font-body)',          // still Inter, different sizing
    size: {
      ...sizes,
      4: 18,
      5: 22,
      6: 26,
      7: 30,
      8: 36,
    },
    weight: {
      4: '600',
      7: '800',
    },
  }),
}
