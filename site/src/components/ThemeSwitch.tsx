import {Switch} from 'tamagui'
import {Moon, Sun} from 'lucide-react'
import {useAppTheme} from '../hooks/useAppTheme'

export function ThemeSwitch() {
  const { theme, toggleTheme } = useAppTheme()
  const isDark = theme === 'dark'

  return (
      <Switch
          size="$3"
          checked={isDark}
          onCheckedChange={toggleTheme}
          borderRadius="$10"
          borderWidth={1}
          borderColor="$borderColor"
          cursor="pointer"
          height={26}
      >
        <Switch.Thumb
            transition="medium"
            bg="$color"
            borderRadius={13}
            items="center"
            justify="center"
            elevation="$2"
            size="$3"
            shadowColor={isDark ? 'rgba(0,0,0,0.6)' : 'rgba(0,0,0,0.15)'}
        >
          {isDark ? (
              <Moon size={15} color="#fff" />
          ) : (
              <Sun size={15} color="#000" />
          )}
        </Switch.Thumb>
      </Switch>
  )
}
