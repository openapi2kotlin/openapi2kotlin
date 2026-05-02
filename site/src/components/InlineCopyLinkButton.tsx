import { useEffect, useRef, useState } from "react";
import { CheckCheckIcon, CopyIcon } from "lucide-react";
import { Text, Tooltip, XStack, YStack } from "tamagui";

type Props = {
  href: string;
  label: string;
};

const COPIED_DURATION_MS = 1200;

export default function InlineCopyLinkButton({ href, label }: Props) {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (timerRef.current !== null) {
        window.clearTimeout(timerRef.current);
      }
    };
  }, []);

  const onCopyLink = async () => {
    const url = new URL(href, window.location.origin).toString();

    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url);
    }

    setCopied(true);
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current);
    }
    timerRef.current = window.setTimeout(() => {
      setCopied(false);
      timerRef.current = null;
    }, COPIED_DURATION_MS);
  };

  return (
    <Tooltip delay={120}>
      <Tooltip.Trigger asChild>
        <Text asChild display="inline">
          <button
            type="button"
            onClick={onCopyLink}
            aria-label={`Copy link to ${label}`}
            style={{
              border: 0,
              background: "transparent",
              padding: 0,
              margin: 0,
              cursor: "pointer",
              font: "inherit",
              color: "inherit",
              verticalAlign: "baseline",
            }}
          >
            <YStack
              display="inline-flex"
              items="center"
              gap="$2"
              px="$2"
              py="$1"
              rounded="$3"
              bg="$pink3"
              hoverStyle={{ bg: "$pink4" }}
              pressStyle={{ bg: "$pink5" }}
            >
              <XStack display="inline-flex" items="center" gap="$2">
                <Text fontWeight="600" color="$color12" fontFamily="$mono">
                  {label}
                </Text>
                <CopyIcon size={14} />
              </XStack>
            </YStack>
          </button>
        </Text>
      </Tooltip.Trigger>
      <Tooltip.Content
        bg="$color1"
        p="$2"
        rounded="$3"
        z={2000}
        enterStyle={{ x: 0, y: -5, opacity: 0, scale: 0.95 }}
        exitStyle={{ x: 0, y: -5, opacity: 0, scale: 0.95 }}
        scale={1}
        x={0}
        y={0}
        opacity={1}
        transition={[
          "medium",
          {
            opacity: {
              overshootClamping: true,
            },
          },
        ]}
      >
        <Text fontSize="$1" whiteSpace="nowrap">
          {copied ? (
            <XStack items="center">
              <CheckCheckIcon size={12} />
              &nbsp;Copied
            </XStack>
          ) : (
            "Copy Link"
          )}
        </Text>
        <Tooltip.Arrow />
      </Tooltip.Content>
    </Tooltip>
  );
}
