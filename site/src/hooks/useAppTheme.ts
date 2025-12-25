import {useContext} from "react";
import {ThemeContext} from "../providers.tsx";

export function useAppTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) {
    throw new Error('useAppTheme must be used inside <Providers>')
  }
  return ctx
}