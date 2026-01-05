import { useMemo, useState } from "react";
import { Button, Stack, Text, Theme, XStack } from "tamagui";
import { CheckCheck } from "lucide-react";

type Props = {
  title: string;
  language?: string;
  code: string;
  lineNumbers?: boolean;
};

type Seg = {
  text: string;
  color?: string;
  tamaguiColor?: string;
};

const PINK = "#d6409f";
const GREEN = "#46916c";
const PURPLE = "#8e4ec6";

function splitLine(line: string): Seg[] {
  const out: Seg[] = [];
  const push = (text: string, color?: string, tamaguiColor?: string) => {
    if (!text) return;
    out.push({ text, color, tamaguiColor });
  };
  const pushDefault = (text: string) => push(text, undefined, "$color");

  let i = 0;

  while (i < line.length) {
    const ch = line[i];

    if (ch === '"') {
      let j = i + 1;
      let escaped = false;
      for (; j < line.length; j++) {
        const c = line[j];
        if (escaped) {
          escaped = false;
          continue;
        }
        if (c === "\\") {
          escaped = true;
          continue;
        }
        if (c === '"') {
          j++;
          break;
        }
      }
      push(line.slice(i, j), GREEN);
      i = j;
      continue;
    }

    if (ch === "[") {
      const end = line.indexOf("]", i + 1);
      if (end >= 0) {
        push(line.slice(i, end + 1), PINK);
        i = end + 1;
        continue;
      }
      pushDefault(ch);
      i++;
      continue;
    }

    if (ch === "{" || ch === "}" || ch === "," || ch === "=") {
      push(ch, PURPLE);
      i++;
      continue;
    }

    if (isIdentStart(ch)) {
      let j = i + 1;
      while (j < line.length && isIdentPart(line[j])) j++;

      const ident = line.slice(i, j);
      const next1 = line[j] ?? "";
      const next2 = line[j + 1] ?? "";

      if (next1 === " " && next2 === "{") {
        push(ident, PINK);
        i = j;
        continue;
      }

      pushDefault(ident);
      i = j;
      continue;
    }

    pushDefault(ch);
    i++;
  }

  return out;
}

function isIdentStart(ch: string) {
  return /[A-Za-z_]/.test(ch);
}

function isIdentPart(ch: string) {
  return /[A-Za-z0-9_]/.test(ch);
}

export default function CodeBlock({
                                    title,
                                    language,
                                    code,
                                    lineNumbers = true,
                                  }: Props) {
  const [copied, setCopied] = useState(false);

  const cleaned = useMemo(() => code.replace(/\s+$/, ""), [code]);
  const lines = useMemo(() => cleaned.split("\n"), [cleaned]);

  const canClipboard =
      typeof navigator !== "undefined" && !!navigator.clipboard?.writeText;

  const onCopy = async () => {
    if (!canClipboard) return;
    await navigator.clipboard.writeText(cleaned);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 900);
  };

  const lineNoWidth = useMemo(() => {
    const digits = String(lines.length).length;
    return Math.max(2, digits) * 10 + 18;
  }, [lines.length]);

  return (
      <Theme name="pink">
        <Stack
            rounded="$6"
            overflow="hidden"
            borderWidth={1}
            borderColor="$borderColor"
            bg="$color2"
        >
          {/* Header stays OUTSIDE the scroll container => copy is always visible */}
          <XStack items="center" justify="space-between" px="$3" py="$2" gap="$2">
            <XStack items="baseline" gap="$2" flex={1} overflow="hidden">
              <Text fontSize="$3" fontWeight="800" numberOfLines={1}>
                {title}
              </Text>
              {language ? (
                  <Text fontSize="$2" opacity={0.7} numberOfLines={1}>
                    {language}
                  </Text>
              ) : null}
            </XStack>

            {canClipboard ? (
                <Button
                    size="$2"
                    onPress={onCopy}
                    icon={copied ? CheckCheck : undefined}
                    bg="$color4"
                    hoverStyle={{ bg: "$color5" }}
                    shrink={0}
                >
                  {copied ? "Copied" : "Copy"}
                </Button>
            ) : null}
          </XStack>

          {/* Code scroll area */}
          <Stack
              px="$3"
              py="$3"
              style={{
                overflowX: "auto",
                WebkitOverflowScrolling: "touch",
              }}
          >
            {/* This wrapper forces the content width to be as wide as the longest line */}
            <Stack style={{ minWidth: "max-content" }}>
              {lines.map((line, idx) => {
                const segs = splitLine(line);
                const lineNumber = idx + 1;

                return (
                    <XStack key={idx} gap="$3" items="flex-start">
                      {lineNumbers ? (
                          <Text
                              fontFamily="$mono"
                              fontSize="$3"
                              lineHeight="$4"
                              opacity={0.45}
                              style={{
                                width: lineNoWidth,
                                textAlign: "right",
                                userSelect: "none",
                                flexShrink: 0,
                              }}
                          >
                            {lineNumber}
                          </Text>
                      ) : null}

                      <Text
                          fontFamily="$mono"
                          fontSize="$3"
                          lineHeight="$4"
                          whiteSpace="pre"
                          color="$color"
                          // do NOT let the code column shrink; let the scroll handle overflow
                          style={{ flexShrink: 0 }}
                      >
                        {segs.length ? (
                            segs.map((s, i2) =>
                                    s.color ? (
                                        <span key={i2} style={{ color: s.color }}>
                            {s.text}
                          </span>
                                    ) : (
                                        <span key={i2} style={{ color: "inherit" }}>
                            {s.text}
                          </span>
                                    )
                            )
                        ) : (
                            <span>{" "}</span>
                        )}
                      </Text>
                    </XStack>
                );
              })}
            </Stack>
          </Stack>
        </Stack>
      </Theme>
  );
}
