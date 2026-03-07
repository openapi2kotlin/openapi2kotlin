import {useEffect, useMemo, useRef, useState} from "react";
import {Button} from "tamagui";
import {CheckCheck} from "lucide-react";

type Props = {
  value: string;
};

const COPIED_DURATION_MS = 900;

export default function CodeCopyButton({ value }: Props) {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (timerRef.current !== null) {
        window.clearTimeout(timerRef.current);
      }
    };
  }, []);

  const canClipboard = useMemo(
    () => typeof navigator !== "undefined" && !!navigator.clipboard?.writeText,
    [],
  );

  if (!canClipboard) {
    return null;
  }

  const onCopy = async () => {
    await navigator.clipboard.writeText(value);
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
    <Button
      size="$2"
      onPress={onCopy}
      icon={copied ? CheckCheck : undefined}
      bg="$color4"
      hoverStyle={{
        bg: "$color5",
      }}
      shrink={0}
    >
      {copied ? "Copied" : "Copy"}
    </Button>
  );
}
