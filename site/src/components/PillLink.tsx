import type { ReactNode } from "react";
import { Text, XStack, YStack } from "tamagui";

type Props = {
  href: string;
  label: ReactNode;
  icon?: ReactNode;
};

export default function PillLink({ href, label, icon }: Props) {
  return (
    <Text asChild>
      <a href={href} target="_blank" rel="noopener noreferrer" style={{ textDecoration: "none" }}>
        <YStack
          asChild
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
          <XStack items="center" gap="$1">
            <Text fontWeight="600" color="$color12" fontFamily="$mono">
              {label} {icon}
            </Text>
          </XStack>
        </YStack>
      </a>
    </Text>
  );
}
