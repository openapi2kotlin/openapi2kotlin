import { useEffect, useMemo, useRef, useState } from 'react'
import { AnimatePresence, Button, Text, Tooltip, XStack, YStack } from 'tamagui'
import { useMedia } from '@tamagui/core'
import { ChevronDown, Tag } from 'lucide-react'

export default function VersionPicker({
  selectedVersion,
  availableVersions,
  latestVersion,
  onSelectVersion,
}: {
  selectedVersion: string
  availableVersions: string[]
  latestVersion: string
  onSelectVersion: (version: string) => void
}) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement | null>(null)
  const media = useMedia()
  const pickerWidth = media.maxXs ? 90 : 130

  const versions = useMemo(() => availableVersions, [availableVersions])
  const tooltipText =
    selectedVersion === latestVersion
      ? 'Latest stable version'
      : `Past version docs (latest: ${latestVersion})`
  const pillTheme = selectedVersion === latestVersion ? "green" : "yellow"

  useEffect(() => {
    if (!open) return
    const handlePointerDown = (event: MouseEvent) => {
      const target = event.target as Node | null
      if (!target) return
      if (!rootRef.current?.contains(target)) {
        setOpen(false)
      }
    }
    document.addEventListener("mousedown", handlePointerDown)
    return () => {
      document.removeEventListener("mousedown", handlePointerDown)
    }
  }, [open])

  return (
    <div ref={rootRef} style={{ position: "relative" }}>
    <YStack position="relative">
      <Tooltip delay={200} placement="bottom">
        <Tooltip.Trigger asChild>
          <Button
            theme={pillTheme}
            size="$1"
            rounded="$10"
            icon={Tag}
            onPress={() => setOpen((v) => !v)}
            pressStyle={{ scale: 0.96 }}
            width={pickerWidth}
          >
            <Button.Text fontFamily="$mono" fontSize="$3" fontWeight="700">
              {selectedVersion}
            </Button.Text>
          </Button>
        </Tooltip.Trigger>
        <Tooltip.Content
          bg="$color1"
          p="$2"
          z={1100}
          rounded="$4"
          enterStyle={{ x: 0, y: -5, opacity: 0, scale: 0.9 }}
          exitStyle={{ x: 0, y: -5, opacity: 0, scale: 0.9 }}
          scale={1}
          x={0}
          y={0}
          opacity={1}
          transition={[
            'medium',
            {
              opacity: {
                overshootClamping: true,
              },
            },
          ]}
        >
          <Text fontSize="$2" whiteSpace="nowrap">
            {tooltipText}
          </Text>
          <Tooltip.Arrow />
        </Tooltip.Content>
      </Tooltip>

      <Button
        unstyled
        position="absolute"
        r={-22}
        t={-1}
        p="$1"
        onPress={() => setOpen((v) => !v)}
        aria-label="Select version"
      >
        <ChevronDown size={20} cursor="pointer" />
      </Button>

      <AnimatePresence>
        {open ? (
          <YStack
            position="absolute"
            t={42}
            l={0}
            minW={pickerWidth}
            bg="$background"
            borderWidth={1}
            borderColor="$borderColor"
            rounded="$4"
            z={200}
            elevation="$2"
            py="$1"
            maxH="50vh"
            overflow="scroll"
            enterStyle={{ x: 0, y: -15, opacity: 0, scale: 0.9 }}
            exitStyle={{ x: 0, y: -15, opacity: 0, scale: 0.9 }}
            scale={1}
            x={0}
            y={0}
            opacity={1}
            transition={[
              'medium',
              {
                opacity: {
                  overshootClamping: true,
                },
              },
            ]}
          >
            {versions.map((version) => (
              <XStack key={version} px="$2" py="$1">
                <Button
                  width="100%"
                  rounded="$3"
                  px="$2"
                  py="$1"
                  size="$2"
                  justify="flex-start"
                  onPress={() => {
                    onSelectVersion(version)
                    setOpen(false)
                  }}
                  bg={version == selectedVersion ? '$color4' : 'transparent'}
                  hoverStyle={{ bg: '$color3' }}
                >
                  <Text fontFamily="$mono" fontSize="$3">
                    {version}
                  </Text>
                </Button>
              </XStack>
            ))}
          </YStack>
        ) : null}
      </AnimatePresence>
    </YStack>
    </div>
  )
}
