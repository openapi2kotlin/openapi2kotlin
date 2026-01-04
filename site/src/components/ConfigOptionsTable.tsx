import { Stack, Text, XStack } from "tamagui";

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
      <Stack gap="$4" theme="blue" mt="$10">
        <Text fontFamily="$heading" fontSize="$4" fontWeight="800">
          {title}
        </Text>

        {/* container has NO borders on sides; just rounding + clip */}
        <Stack rounded="$6" overflow="hidden">
          {/* header */}
          <XStack bg="$color3" px="$4" py="$3" borderBottomWidth={1} borderBottomColor="$color3">
            <HeaderCell flex={COLS.property} label="Property" />
            <HeaderCell flex={COLS.description} label="Description" />
            <HeaderCell flex={COLS.example} label="Example" />
          </XStack>

          {/* body */}
          <Stack bg="$color2">
            {rows.map((r, idx) => (
                <Row key={idx} row={r} isLast={idx === rows.length - 1} />
            ))}
          </Stack>
        </Stack>
      </Stack>
  );
}

function HeaderCell({ label, flex }: { label: string; flex: number }) {
  return (
      <Stack
          // IMPORTANT: flexBasis 0 makes widths stable across rows on web
          style={{ flexBasis: 0 }}
          flex={flex}
          minW={0}
          pr="$4"
      >
        <Text fontSize="$4" fontWeight="800" opacity={0.95}>
          {label}
        </Text>
      </Stack>
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
          // no gaps; the padding on cells controls spacing
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
          <ExampleBlock text={row.example} />
        </Cell>
      </XStack>
  );
}

function Cell({ children, flex }: { children: React.ReactNode; flex: number }) {
  return (
      <Stack
          style={{ flexBasis: 0 }}
          flex={flex}
          minW={0}
          // spacing between columns without borders:
          pr="$4"
          justify="center"
      >
        {children}
      </Stack>
  );
}

function CodePill({ text }: { text: string }) {
  return (
      <Stack
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
      </Stack>
  );
}

function ExampleBlock({ text }: { text: string }) {
  return (
      <Stack
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
      </Stack>
  );
}
