import type { ReactNode } from "react";
import {useMemo, useRef, useState} from "react";
import {Button, Image, Text, XStack, YStack} from "tamagui";
import { useMedia } from "@tamagui/core";

export type SegmentedOption<T extends string> = {
  value: T;
  label: string;
  icon?: ReactNode;
  iconSrc?: string;
  iconAlt?: string;
  iconSize?: number;
};

export default function SegmentedControl<T extends string>({
                                                             options,
                                                             value,
                                                             onChange,
                                                             hideLabelsOnMobile = false,
                                                           }: {
  options: SegmentedOption<T>[];
  value: T;
  onChange: (value: T) => void;
  hideLabelsOnMobile?: boolean;
}) {
  const media = useMedia();
  const [hoveredValue, setHoveredValue] = useState<T | null>(null);
  const [hoverRenderIndex, setHoverRenderIndex] = useState(0);
  const [hoverCanAnimateTransform, setHoverCanAnimateTransform] = useState(true);
  const hoverActivateRaf = useRef<number | null>(null);

  const segmentCount = Math.max(1, options.length);
  const showIconsOnly = hideLabelsOnMobile && media.maxMd;
  const insetPx = 4;
  const gapPx = 4;

  const selectedIndex = Math.max(0, options.findIndex((it) => it.value === value));
  const hoveredIndex = hoveredValue ? options.findIndex((it) => it.value === hoveredValue) : -1;

  const segmentWidth = useMemo(
      () => `calc((100% - ${(segmentCount - 1) * gapPx}px) / ${segmentCount})`,
      [gapPx, segmentCount],
  );

  const offsetFor = (index: number) =>
      `calc(${index} * ((100% - ${(segmentCount - 1) * gapPx}px) / ${segmentCount} + ${gapPx}px))`;

  const setHovered = (nextValue: T) => {
    const nextIndex = options.findIndex((it) => it.value === nextValue);
    if (nextIndex < 0) return;

    if (hoveredValue == null) {
      setHoverCanAnimateTransform(false);
      setHoverRenderIndex(nextIndex);
      setHoveredValue(nextValue);
      if (hoverActivateRaf.current != null) {
        window.cancelAnimationFrame(hoverActivateRaf.current);
      }
      hoverActivateRaf.current = window.requestAnimationFrame(() => {
        setHoverCanAnimateTransform(true);
        hoverActivateRaf.current = null;
      });
      return;
    }

    setHoveredValue(nextValue);
    setHoverRenderIndex(nextIndex);
  };

  const clearHovered = () => {
    setHoveredValue(null);
    if (hoverActivateRaf.current != null) {
      window.cancelAnimationFrame(hoverActivateRaf.current);
      hoverActivateRaf.current = null;
    }
    setHoverCanAnimateTransform(true);
  };

  return (
      <YStack width="100%" borderWidth={1} borderColor="$color3" rounded="$9" bg="$background" elevation="$2">
        <YStack position="relative" p={insetPx}>
          <YStack position="absolute" l={insetPx} r={insetPx} t={insetPx} b={insetPx} pointerEvents="none">
            <YStack
                position="absolute"
                t={0}
                b={0}
                rounded="$10"
                bg="$color5"
                opacity={hoveredIndex >= 0 ? 0.5 : 0}
                transition={hoverCanAnimateTransform ? "mediumLessBouncy" : "0ms"}
                style={{
                  left: offsetFor(Math.max(0, hoverRenderIndex)),
                  width: segmentWidth,
                }}
            />

            <YStack
                position="absolute"
                t={0}
                b={0}
                rounded="$10"
                bg="$color4"
                opacity={0.9}
                transition="mediumLessBouncy"
                style={{
                  left: offsetFor(selectedIndex),
                  width: segmentWidth,
                }}
            />
          </YStack>

        <XStack gap={gapPx} onMouseLeave={clearHovered}>
          {options.map((option) => (
                <Button
                    key={option.value}
                    unstyled
                    height={32}
                    rounded="$10"
                    flex={1}
                    flexBasis={0}
                    items="center"
                    justify="center"
                    cursor="pointer"
                    style={{minWidth: 0}}
                    onPress={() => onChange(option.value)}
                    onMouseEnter={() => setHovered(option.value)}
                    onFocus={() => setHovered(option.value)}
                    onBlur={clearHovered}
                    hoverStyle={{bg: "transparent"}}
                    pressStyle={{scale: 0.98}}
                >
                  <XStack
                    items="center"
                    justify="center"
                    gap={showIconsOnly ? 0 : "$2"}
                    style={{ minWidth: 0 }}
                  >
                    {option.icon ? (
                      <XStack style={{ flexShrink: 0 }}>
                        {option.icon}
                      </XStack>
                    ) : option.iconSrc ? (
                      <Image
                        src={option.iconSrc}
                        alt={option.iconAlt ?? `${option.label} logo`}
                        width={option.iconSize ?? 18}
                        height={option.iconSize ?? 18}
                        resizeMode="contain"
                        style={{ flexShrink: 0 }}
                      />
                    ) : null}
                    {!showIconsOnly ? (
                      <Text fontSize="$4" fontWeight="700" color="$color12" style={{textAlign: "center"}}>
                        {option.label}
                      </Text>
                    ) : null}
                  </XStack>
                </Button>
            ))}
          </XStack>
        </YStack>
      </YStack>
  );
}
