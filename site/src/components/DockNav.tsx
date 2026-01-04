import {Stack, XStack} from "tamagui";
import Logo from "./Logo";
import {ThemeSwitch} from "./ThemeSwitch";
import {useAppTheme} from "../hooks/useAppTheme.ts";
import VersionBadge from "./VersionBadge.tsx";
import GitHubLink from "./GitHubLink.tsx";

export default function DockNav() {
  const { theme } = useAppTheme()
  const isDark = theme === 'dark'
  return (
      <Stack
          $platform-web={{
            position: 'fixed',
          }}
          t="$3"
          l={0}
          r={0}
          z={50}
          items="center"
          px="$4"
          pointerEvents="none"
      >
        <XStack
            pointerEvents="auto"
            items="center"
            justify="space-between"
            gap="$4"
            px="$4"
            py="$2"
            maxW={980}
            width="100%"
            rounded="$10"
            bg={isDark ? "rgba(39,39,39,0.3)" : "rgba(223,223,223,0.3)"}
            style={{
              backdropFilter: "blur(14px)",
              WebkitBackdropFilter: "blur(14px)",
              boxShadow: "0 12px 40px rgba(0,0,0,0.10)",
            }}
        >
          <XStack width={120}>
            <VersionBadge version="0.10.0" />
          </XStack>

          <Logo />

          <XStack width={120} justify="space-between" items="center" gap="$2">
            <GitHubLink/>
            <ThemeSwitch />
          </XStack>
        </XStack>
      </Stack>
  );
}
