import './App.css'
import {Stack, XStack,} from "tamagui";
import Logo from "./components/Logo.tsx";
import {ThemeSwitch} from "./components/ThemeSwitch.tsx";

function App() {
  return (
      <Stack flex={1} height="100vh" bg="$primary">
        <XStack
            p="$3"
            items="center"
            justify="space-between"
            width="100%"
            bg="$primary"
            gap="$4"
        >
          <Stack width={100}/>
          <Logo/>
          <XStack
              justify="space-between"
              width={100}
          >
            <div/>
            <ThemeSwitch/>
          </XStack>
        </XStack>
        <Stack
            flex={1}
            position="relative"
            style={{
              overscrollBehaviorX: "none",
              touchAction: "manipulation",
              overflow: "hidden",
            }}
        >
          {/*TODO: OpenAPI 2 Kotlin plugin docs ...*/}
        </Stack>
      </Stack>
  )
}

export default App
