import { Stack, Text } from "tamagui"

export function HeroHeading() {
  return (
      <Stack items="center" gap="$2">
        <Text
            fontFamily="$heading"
            fontSize={120}
            lineHeight={110}
            fontWeight="800"
            letterSpacing={-2}
        >
          OpenAPI
          <Text
              fontSize={64}
              verticalAlign="top"
              color="$pink10"
          >
            ²
          </Text>
        </Text>

        <Text
            fontFamily="$heading"
            fontSize={96}
            lineHeight={96}
            fontWeight="900"
            letterSpacing={-1}
            color="$color12"
        >
          Kotlin
        </Text>
      </Stack>
  )
}
