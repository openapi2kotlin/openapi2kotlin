import {Text, XStack, YStack} from "tamagui";

export type ConfigRow = {
  property: string;
  description: string;
  example: string;
};

type Props = {
  title?: string;
  rows: ConfigRow[];
};

const COLS = {
  property: 1.4,
  description: 1.2,
  example: 2.2,
};

export default function ConfigOptionsTable({
                                             title = "Configuration options",
                                             rows,
                                           }: Props) {
  return (
      <YStack gap="$4" theme="blue" mt="$10">
        <Text fontFamily="$heading" fontSize="$4" fontWeight="800">
          {title}
        </Text>

        {/* Mobile: cards */}
        <YStack
            display="flex"
            gap="$3"
            $md={{ display: "none" }}
        >
          {rows.map((r, idx) => (
              <ConfigCard key={idx} row={r} />
          ))}
        </YStack>

        {/* Desktop+: table */}
        <YStack
            display="none"
            $md={{ display: "flex" }}
            rounded="$6"
            overflow="hidden"
        >
          <XStack bg="$color3" px="$4" py="$3" borderBottomWidth={1} borderBottomColor="$color3">
            <HeaderCell flex={COLS.property} label="Property" />
            <HeaderCell flex={COLS.description} label="Description" />
            <HeaderCell flex={COLS.example} label="Example" />
          </XStack>

          <YStack bg="$color2">
            {rows.map((r, idx) => (
                <Row key={idx} row={r} isLast={idx === rows.length - 1} />
            ))}
          </YStack>
        </YStack>
      </YStack>
  );
}

function ConfigCard({ row }: { row: ConfigRow }) {
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

        <XStack items="center" justify="flex-start" gap="$3" mb="$3">
          <CodePill text={row.property} />
        </XStack>

        <YStack gap="$2">
          <Text fontSize="$2" opacity={0.7} fontWeight="700">
            Description
          </Text>
          <Text fontSize="$4" lineHeight="$5" opacity={0.9}>
            {row.description}
          </Text>
        </YStack>

        <YStack gap="$2">
          <Text fontSize="$2" opacity={0.7} fontWeight="700">
            Example
          </Text>
          {/* This is intentionally scrollable horizontally on mobile */}
          <ExampleBlockScrollable text={row.example} />
        </YStack>
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
          <CodePill text={row.property} />
        </Cell>

        <Cell flex={COLS.description}>
          <Text fontSize="$4" lineHeight="$5" opacity={0.9}>
            {row.description}
          </Text>
        </Cell>

        <Cell flex={COLS.example}>
          {/* Desktop can wrap; it has space */}
          <ExampleBlock text={row.example} />
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

function CodePill({ text }: { text: string }) {
  return (
      <YStack
          bg="$color4"
          borderWidth={1}
          borderColor="$color5"
          rounded="$4"
          px="$2"
          py="$1"
          self="flex-start"
          maxW="100%"
      >
        <Text fontFamily="$mono" fontSize="$1" lineHeight="$4" numberOfLines={1}>
          {text}
        </Text>
      </YStack>
  );
}

function ExampleBlock({ text }: { text: string }) {
  return (
      <YStack
          bg="$color1"
          borderWidth={1}
          borderColor="$color5"
          rounded="$4"
          px="$2"
          py="$1"
          maxW="100%"
      >
        <Text fontFamily="$mono" fontSize="$1" lineHeight="$4" whiteSpace="pre-wrap">
          {text}
        </Text>
      </YStack>
  );
}

/**
 * Mobile-friendly: show examples as one-line code that scrolls horizontally,
 * similar to Tamagui docs (code blocks don’t wrap aggressively on mobile).
 */
function ExampleBlockScrollable({ text }: { text: string }) {
  return (
      <YStack
          bg="$color1"
          borderWidth={1}
          borderColor="$color5"
          rounded="$4"
          px="$2"
          py="$2"
          style={{
            overflowX: "auto",
            WebkitOverflowScrolling: "touch",
          }}
      >
        <YStack style={{ minWidth: "max-content" }}>
          <Text fontFamily="$mono" fontSize="$1" lineHeight="$4" whiteSpace="pre">
            {text}
          </Text>
        </YStack>
      </YStack>
  );
}
