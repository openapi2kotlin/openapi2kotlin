// src/providers.tsx
import type { ReactNode } from 'react'
import { createContext, useEffect, useState } from 'react'
import { TamaguiProvider } from 'tamagui'
import { ToastProvider, ToastViewport } from '@tamagui/toast'
import { config } from '../tamagui/tamagui.config'
import AppToast from './components/AppToast'

type ColorScheme = 'light' | 'dark'

type ThemeContextValue = {
  theme: ColorScheme
  setTheme: (theme: ColorScheme) => void
  toggleTheme: () => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)

const STORAGE_KEY = 'diagram-theme'

function getInitialTheme(): ColorScheme {
  if (typeof window === 'undefined') return 'dark'

  // 1) user preference (like NextThemeProvider localStorage)
  const stored = window.localStorage.getItem(STORAGE_KEY) as ColorScheme | null
  if (stored === 'light' || stored === 'dark') return stored

  // 2) system preference (like defaultTheme="system" + enableSystem)
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  return prefersDark ? 'dark' : 'light'
}

export function Providers({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ColorScheme>(() => getInitialTheme())

  useEffect(() => {
    if (typeof window === 'undefined') return

    // persist like NextThemeProvider does
    window.localStorage.setItem(STORAGE_KEY, theme)

    // optional: raw CSS hooks: html[data-theme="dark"] { ... }
    document.documentElement.dataset.theme = theme

    // also update color-scheme so scrollbars, etc. match
    document.documentElement.style.colorScheme = theme
  }, [theme])

  const setTheme = (next: ColorScheme) => setThemeState(next)
  const toggleTheme = () =>
      setThemeState((prev) => (prev === 'dark' ? 'light' : 'dark'))

  return (
      <ThemeContext.Provider value={{ theme, setTheme, toggleTheme }}>
        <TamaguiProvider config={config} defaultTheme={theme}>
          <ToastProvider swipeDirection="vertical" duration={2000} native={false}>
            {children}
            <AppToast />
            <ToastViewport
                flexDirection="column-reverse"
                bottom={0}
                left={0}
                right={0}
                multipleToasts
            />
          </ToastProvider>
        </TamaguiProvider>
      </ThemeContext.Provider>
  )
}
