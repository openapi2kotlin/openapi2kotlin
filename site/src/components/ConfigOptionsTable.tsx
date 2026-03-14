import {Text, Tooltip, XStack, YStack} from "tamagui";
import type {ConfigRow} from "../model/version-docs";
import SectionCopyLinkButton from "./SectionCopyLinkButton";
import { useMedia } from "@tamagui/core";

type Props = {
  title?: string;
  rows: ConfigRow[];
};

type ConfigSection = {
  key: "root" | "model" | "client" | "server";
  title?: string;
  anchorId: string;
  rows: ConfigRow[];
};

const COLS = {
  property: 2,
  description: 2.8,
  values: 1.3,
  defaultValue: 1.6,
};

export default function ConfigOptionsTable({
  rows,
}: Props) {
  const sections = toSections(rows);
  const media = useMedia();
  const isMobile = media.maxMd;

  return (
    <YStack gap="$4" theme="blue" mt="$2">
      <YStack display="flex" gap="$3" $md={{ display: "none" }}>
        {sections.map((section) => (
          <YStack key={`mobile-${section.key}`} id={isMobile ? section.anchorId : undefined} gap="$3">
            {section.title ? (
              <SectionHeader title={section.title} anchorId={section.anchorId} mobile />
            ) : null}
            {section.rows.map((r, idx) => (
              <ConfigCard key={`${section.key}-${idx}-${r.property}`} row={r} />
            ))}
          </YStack>
        ))}
      </YStack>

      <YStack
        display="none"
        $md={{ display: "flex" }}
        gap="$9"
      >
        {sections.map((section) => (
          <YStack key={`desktop-${section.key}`} id={!isMobile ? section.anchorId : undefined} gap="$2">
            {section.title ? (
              <SectionHeader title={section.title} anchorId={section.anchorId} />
            ) : null}

            <YStack rounded="$6" borderWidth={1} borderColor="$color5" overflow="hidden">
              <XStack
                bg="$color3"
                px="$4"
                py="$3"
                borderBottomWidth={1}
                borderBottomColor="$color3"
              >
                <HeaderCell flex={COLS.property} label="Property" />
                <HeaderCell flex={COLS.description} label="Description" />
                <HeaderCell flex={COLS.values} label="Values" />
                <HeaderCell flex={COLS.defaultValue} label="Default" />
              </XStack>

              <YStack bg="$color2">
                {section.rows.map((r, idx) => (
                  <Row key={`${section.key}-${idx}-${r.property}`} row={r} isLast={idx === section.rows.length - 1} />
                ))}
              </YStack>
            </YStack>
          </YStack>
        ))}
      </YStack>
    </YStack>
  );
}

function SectionHeader({
  title,
  anchorId,
  mobile = false,
}: {
  title: string;
  anchorId: string;
  mobile?: boolean;
}) {
  return (
    <XStack mt={mobile ? 0 : "$4"} mb="$3" items="center" justify="space-between" gap="$3">
      <Text
        fontFamily={mobile ? "$heading" : "$mono"}
        fontSize={mobile ? "$5" : "$8"}
        fontWeight="700"
        opacity={0.92}
      >
        {title}
      </Text>
      <SectionCopyLinkButton anchorId={anchorId} title={title} />
    </XStack>
  );
}

function toSections(rows: ConfigRow[]): ConfigSection[] {
  const rootRows = rows.filter((r) => !r.property.includes("."));
  const modelRows = rows
    .filter((r) => r.property.startsWith("model."))
    .map((r) => ({ ...r, property: r.property.replace(/^model\./, "") }));
  const clientRows = rows
    .filter((r) => r.property.startsWith("client."))
    .map((r) => ({ ...r, property: r.property.replace(/^client\./, "") }));
  const serverRows = rows
    .filter((r) => r.property.startsWith("server."))
    .map((r) => ({ ...r, property: r.property.replace(/^server\./, "") }));

  const sections: ConfigSection[] = [
    { key: "root", title: "openapi2kotlin", anchorId: "openapi2kotlin", rows: rootRows },
    { key: "model", title: "openapi2kotlin.model", anchorId: "openapi2kotlin-model", rows: modelRows },
    { key: "client", title: "openapi2kotlin.client", anchorId: "openapi2kotlin-client", rows: clientRows },
    { key: "server", title: "openapi2kotlin.server", anchorId: "openapi2kotlin-server", rows: serverRows },
  ];

  return sections.filter((section) => section.rows.length > 0);
}

function ConfigCard({ row }: { row: ConfigRow }) {
  const values = row.values ?? "";
  const defaultValue = row.default ?? "";
  const hasValues = splitValues(values).length > 0;
  const hasDefault = defaultValue.trim().length > 0;

  return (
    <YStack
      bg="$color2"
      borderWidth={1}
      borderColor="$color3"
      rounded="$6"
      px="$4"
      py="$4"
      gap="$3"
    >
      <XStack items="center" justify="flex-start" gap="$3" mb="$1">
        <CodePill text={row.property} required={row.required} />
      </XStack>

      <DescriptionBlock title="Description" text={row.description} />
      {hasValues ? <LabelBlock title="Values" text={values} isValues /> : null}
      {hasDefault ? <DefaultLabelBlock title="Default" text={defaultValue} /> : null}
    </YStack>
  );
}

function LabelBlock({
  title,
  text,
  isValues,
}: {
  title: string;
  text: string;
  isValues?: boolean;
}) {
  const valueItems = isValues ? splitValues(text) : [];
  return (
    <YStack gap="$2">
      <Text fontSize="$2" opacity={0.7} fontWeight="700">
        {title}
      </Text>
      {isValues ? <PillList items={valueItems} mono /> : null}
      {!isValues ? <ExampleBlock text={text} /> : null}
    </YStack>
  );
}

function HeaderCell({ label, flex }: { label: string; flex: number }) {
  return (
    <YStack style={{ flexBasis: 0 }} flex={flex} minW={0} pr="$4">
      <Text fontSize="$4" fontWeight="800" opacity={0.95}>
        {label}
      </Text>
    </YStack>
  );
}

function Row({ row, isLast }: { row: ConfigRow; isLast: boolean }) {
  return (
    <XStack
      px="$4"
      py="$4"
      bg="$color2"
      borderBottomWidth={isLast ? 0 : 1}
      borderBottomColor="$color3"
    >
      <Cell flex={COLS.property}>
        <CodePill text={row.property} required={row.required} />
      </Cell>

      <Cell flex={COLS.description}>
        <DescriptionBlock text={row.description} />
      </Cell>

      <Cell flex={COLS.values}>
        <PillList items={splitValues(row.values || "")} mono />
      </Cell>

      <Cell flex={COLS.defaultValue}>
        <DefaultBlock text={row.default || ""} />
      </Cell>

    </XStack>
  );
}

function Cell({ children, flex }: { children: React.ReactNode; flex: number }) {
  return (
    <YStack style={{ flexBasis: 0 }} flex={flex} minW={0} pr="$4" justify="center">
      {children}
    </YStack>
  );
}

function CodePill({ text, required }: { text: string; required?: boolean }) {
  const content = (
    <YStack
      position="relative"
      bg="$color4"
      borderWidth={1}
      borderColor="$color5"
      rounded="$4"
      px="$2"
      py="$1"
      self="flex-start"
      maxW="100%"
      cursor={required ? "pointer" : "default"}
    >
      {required ? (
        <Text
          position="absolute"
          t={-6}
          r={-6}
          color="#ff4d4f"
          fontSize="$2"
          fontWeight="800"
        >
          *
        </Text>
      ) : null}
      <Text
        fontFamily="$mono"
        fontSize="$1"
        lineHeight="$4"
        style={{ overflowWrap: "anywhere" }}
      >
        {text}
      </Text>
    </YStack>
  );

  if (!required) {
    return content;
  }

  return (
      <Tooltip delay={120}>
        <Tooltip.Trigger asChild>{content}</Tooltip.Trigger>
        <Tooltip.Content
            theme="red"
            bg="$color1"
            p="$2" rounded="$3"
            z={2000}
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
          <Text fontSize="$1" whiteSpace="nowrap">
            Required
          </Text>
          <Tooltip.Arrow />
        </Tooltip.Content>
      </Tooltip>
  );
}

function ExampleBlock({ text, mono = false }: { text: string; mono?: boolean }) {
  return (
    <YStack bg="$color1" borderWidth={1} borderColor="$color5" rounded="$4" px="$2" py="$1" maxW="100%">
      <Text fontFamily={mono ? "$mono" : "$body"} fontSize="$1" lineHeight="$4" whiteSpace="pre-wrap">
        {stripInlineCodeMarkers(text)}
      </Text>
    </YStack>
  );
}

function splitValues(values: string): string[] {
  return values
    .split(",")
    .map((v) => v.trim())
    .filter((v) => v.length > 0 && v !== "-");
}

function PillList({ items, mono = false }: { items: string[]; mono?: boolean }) {
  if (items.length === 0) {
    return null;
  }
  return (
    <YStack gap="$2" maxW="100%">
      {items.map((item, idx) => (
        <ExampleBlock key={`${item}-${idx}`} text={item} mono={mono} />
      ))}
    </YStack>
  );
}

function DefaultLabelBlock({ title, text }: { title: string; text: string }) {
  return (
    <YStack gap="$2">
      <Text fontSize="$2" opacity={0.7} fontWeight="700">
        {title}
      </Text>
      <DefaultBlock text={text} />
    </YStack>
  );
}

function DefaultBlock({ text }: { text: string }) {
  const normalized = text.trim();
  if (normalized.length === 0) {
    return null;
  }

  const rules = splitArrowRules(normalized);
  if (rules.length > 0) {
    return (
      <YStack gap="$2" maxW="100%">
        {rules.map((rule, idx) => (
          <YStack
            key={`${rule.left}-${rule.right}-${idx}`}
            gap="$2"
            py="$2"
            borderBottomWidth={idx === rules.length - 1 ? 0 : 1}
            borderBottomColor="$color4"
          >
            <Text fontFamily="$mono" fontSize="$2" opacity={0.9} whiteSpace="nowrap">
              {rule.left} →
            </Text>
            <YStack pl="$1" maxW="100%">
              <ExampleBlock text={rule.right} mono />
            </YStack>
          </YStack>
        ))}
      </YStack>
    );
  }
  return <ExampleBlock text={normalized} mono />;
}

function splitArrowRules(text: string): Array<{ left: string; right: string }> {
  if (!text.includes("->")) return [];
  return text
    .split(",")
    .map((part) => part.trim())
    .map((part) => {
      const [leftRaw, rightRaw] = part.split("->").map((x) => x?.trim() ?? "");
      return { left: leftRaw, right: rightRaw };
    })
    .filter((x) => x.left.length > 0 && x.right.length > 0);
}

function DescriptionBlock({
  text,
  title,
}: {
  text: string;
  title?: string;
}) {
  const parts = splitDescriptionExample(text);
  return (
    <YStack gap="$2">
      {title ? (
        <Text fontSize="$2" opacity={0.7} fontWeight="700">
          {title}
        </Text>
      ) : null}
      <Text fontSize="$4" lineHeight="$5" opacity={0.9}>
        <InlineCodeText text={parts.description} />
      </Text>
      {parts.hint ? (
        <YStack gap="$1" maxW="100%">
          <Text fontSize="$2" opacity={0.75}>
            {parts.hintPrefix}
          </Text>
          <ExampleBlock text={parts.hint} mono />
        </YStack>
      ) : null}
    </YStack>
  );
}

function splitDescriptionExample(text: string): {
  description: string;
  hintPrefix: "e.g." | "i.e.";
  hint: string;
} | {
  description: string;
  hintPrefix?: never;
  hint?: never;
} {
  const normalized = text.trim();
  const fencedMatch = normalized.match(/\b(e\.g\.|i\.e\.)\s*```([\s\S]+?)```$/i);
  if (fencedMatch && fencedMatch.index != null) {
    const marker = fencedMatch[1].toLowerCase() === "i.e." ? "i.e." : "e.g.";
    const before = normalized.slice(0, fencedMatch.index).trim().replace(/[,;:]$/, "");
    const after = fencedMatch[2].trim();
    if (before.length > 0 && after.length > 0) {
      return {
        description: before,
        hintPrefix: marker,
        hint: after,
      };
    }
  }

  const match = normalized.match(/\b(e\.g\.|i\.e\.)\s*(.+)$/i);
  if (!match || match.index == null) {
    return { description: normalized };
  }

  const marker = match[1].toLowerCase() === "i.e." ? "i.e." : "e.g.";
  const before = normalized.slice(0, match.index).trim().replace(/[,;:]$/, "");
  const after = match[2].trim().replace(/[.]$/, "");
  if (before.length === 0 || after.length === 0) {
    return { description: normalized };
  }
  return {
    description: before,
    hintPrefix: marker,
    hint: after,
  };
}

function InlineCodeText({ text }: { text: string }) {
  const segments = splitInlineCodeSegments(text);
  return (
    <>
      {segments.map((segment, index) =>
        segment.kind === "code" ? (
          <Text
            key={`${segment.value}-${index}`}
            fontFamily="$mono"
            fontSize="$2"
            bg="$color1"
            borderWidth={1}
            borderColor="$color5"
            rounded="$4"
            px="$2"
            py="$1"
            lineHeight="$4"
            opacity={0.95}
          >
            {segment.value}
          </Text>
        ) : (
          <Text key={`${segment.value}-${index}`}>
            {segment.value}
          </Text>
        ),
      )}
    </>
  );
}

function splitInlineCodeSegments(text: string): Array<{ kind: "text" | "code"; value: string }> {
  if (!text.includes("`")) {
    return [{ kind: "text", value: text }];
  }

  const parts = text.split(/(`[^`]+`)/g).filter((part) => part.length > 0);
  return parts.map((part) =>
    part.startsWith("`") && part.endsWith("`")
      ? { kind: "code" as const, value: part.slice(1, -1) }
      : { kind: "text" as const, value: part },
  );
}

function stripInlineCodeMarkers(text: string): string {
  return text.replace(/`([^`]+)`/g, "$1");
}
