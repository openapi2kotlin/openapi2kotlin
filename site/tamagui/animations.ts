import { createAnimations } from '@tamagui/animations-css'

// const overshootSoft   = 'cubic-bezier(.14,.64,.5,1.07)'
const overshootMedium = 'cubic-bezier(.14,.64,.54,1.13)'
const overshootStrong = 'cubic-bezier(0.18, 1.60, 0.40, 1.0)'

const smooth = 'cubic-bezier(0.22, 1, 0.36, 1)'

const bouncySmooth = `linear(0, 0.023 1.7%, 0.098 3.8%, 0.551 12.3%, 0.749 16.9%, 0.887 21.8%, 0.934 24.4%, 0.97 27.2%, 1.002 32%, 1.014 38.1%, 1.001 63.6%, 1)`

export const animations = createAnimations({
  '75ms': `${smooth} 75ms`,
  '100ms': `${smooth} 100ms`,
  '200ms': `${smooth} 200ms`,

  // ↓ slower + bouncy
  bouncy:      `${bouncySmooth} 1100ms`,
  bouncier:    `${overshootMedium} 380ms`,
  superBouncy: `${overshootStrong} 440ms`,

  springy: 'spring-settle 380ms cubic-bezier(0.15, 1.0, 0.30, 1.0)',

  overlay: `cubic-bezier(.34,.03,.89,.71) 550ms`,
  lazy: `ease-in 1150ms`,

  // restore more “normal” speeds
  medium: `ease-in 320ms`,
  slow: `ease-in 550ms`,
  quick: `${smooth} 240ms`,
  quicker: `${smooth} 200ms`,
  quickest: `${smooth} 160ms`,
  tooltip: `${smooth} 260ms`,
})
