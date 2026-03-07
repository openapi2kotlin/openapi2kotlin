import {useEffect, useRef, useState} from "react";
import {Button, type ButtonProps, Text, Tooltip, XStack} from "tamagui";
import {CheckCheckIcon, LinkIcon} from "lucide-react";

interface Props extends ButtonProps {
  anchorId: string;
  title: string;
};

const COPIED_DURATION_MS = 1200;

export default function SectionCopyLinkButton({ anchorId, title, ...props }: Props) {
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
    const url = `${window.location.origin}${window.location.pathname}${window.location.search}#${anchorId}`;
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url);
    } else {
      window.history.replaceState(null, "", `#${anchorId}`);
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
        <Button
          {...props}
          onPress={onCopyLink}
          size="$2"
          bg="transparent"
          borderWidth={0}
          icon={LinkIcon}
          hoverStyle={{ bg: "$color3" }}
          pressStyle={{ bg: "$color4" }}
          aria-label={`Copy link to ${title}`}
        />
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
          {copied ? (<XStack items="center"><CheckCheckIcon size={12} />&nbsp;Copied</XStack>) : "Copy Link"}
        </Text>
        <Tooltip.Arrow />
      </Tooltip.Content>
    </Tooltip>
  );
}
