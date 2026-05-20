import {H2, Text, YStack} from "tamagui";
import InlineCopyLinkButton from "./InlineCopyLinkButton.tsx";

type Props = {
  currentLlmsHref: string;
};

export default function LlmsBanner({currentLlmsHref}: Props) {
  return (
    <YStack gap="$3" position="relative" mt="$12">
      <H2 id="llms">AI / LLMs</H2>

      <Text fontSize="$6" color="$color11" opacity={0.85} mb="$2" lineHeight="$7">
        If you're using openapi2kotlin in your project and want to give an agent or LLM the right context, point it at{" "}
        <InlineCopyLinkButton
          href={currentLlmsHref}
          label="llms.txt"
        />{" "}
        for the machine-friendly documentation.
      </Text>
    </YStack>
  );
}
