import type { ReactNode } from "react";
import { ExternalLink } from "lucide-react";
import { Anchor, Text, XStack, YStack } from "tamagui";
import Logo from "./Logo";
import { MENU_ITEMS } from "./contentsMenuItems";

const EXTERNAL_LINKS = [
  {
    label: "GitHub",
    href: "https://github.com/openapi2kotlin/openapi2kotlin",
  },
  {
    label: "Maven Central",
    href: "https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin",
  },
];

function scrollToSection(id: string) {
  const el = document.getElementById(id);
  if (!el) return;

  const bodyIsScrollable =
    document.body.scrollHeight > document.body.clientHeight &&
    getComputedStyle(document.body).overflowY !== "visible";
  const scrollContainer: Window | HTMLElement =
    bodyIsScrollable
      ? document.body
      : ((document.scrollingElement as HTMLElement | null) ?? window);
  const scrollTop =
    scrollContainer === window
      ? window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0
      : (scrollContainer as HTMLElement).scrollTop;
  const top = el.getBoundingClientRect().top + scrollTop - 120;

  if (scrollContainer === window) {
    window.scrollTo({ top: Math.max(0, top), behavior: "smooth" });
  } else {
    scrollContainer.scrollTo({ top: Math.max(0, top), behavior: "smooth" });
  }

  window.history.replaceState(null, "", `#${id}`);
}

function FooterTitle({ children }: { children: ReactNode }) {
  return (
    <Text
      fontFamily="$mono"
      fontSize="$4"
      color="$color11"
      opacity={0.6}
      letterSpacing={0.2}
      textTransform="uppercase"
    >
      {children}
    </Text>
  );
}

function formatGeneratedAt(generatedAt: string) {
  const date = new Date(generatedAt);
  if (Number.isNaN(date.getTime())) return generatedAt;

  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  }).format(date);
}

export default function Footer({
  stacked = false,
  selectedVersion,
  selectedVersionGeneratedAt,
  latestVersion,
  onSelectLatestVersion,
}: {
  stacked?: boolean
  selectedVersion: string
  selectedVersionGeneratedAt: string
  latestVersion: string
  onSelectLatestVersion: () => void
}) {
  const footerAlign = "flex-start";

  return (
    <YStack
      mt="$12"
      pt="$14"
      pb="$6"
      borderTopWidth={1}
      borderTopColor="$color5"
      gap="$7"
      width="100%"
    >
      <YStack
        justify="flex-start"
        items={stacked ? "center" : "flex-start"}
        gap={stacked ? "$10" : "$12"}
        flexDirection={stacked ? "column" : "row"}
        width="100%"
      >
        <YStack gap="$3" minW={200} items={stacked ? "center" : "flex-start"}>
          <Logo />
          <Text
            fontFamily="$mono"
            fontSize="$3"
            color="$color11"
            opacity={0.55}
          >
            openapi2kotlin
          </Text>
          <Text
            fontFamily="$mono"
            fontSize="$2"
            color="$color11"
            opacity={0.55}
          >
            {formatGeneratedAt(selectedVersionGeneratedAt)}
          </Text>
          <Text
            fontFamily="$mono"
            fontSize="$2"
            color="$color11"
            opacity={0.55}
          >
            v{selectedVersion}
          </Text>
        </YStack>

        <YStack
          justify="flex-start"
          gap="$10"
          flexDirection={stacked ? "column" : "row"}
          flexWrap="nowrap"
          width={stacked ? "100%" : "auto"}
          items={stacked ? "center" : "flex-start"}
        >
          <YStack
            gap="$4"
            items={footerAlign}
          >
            <YStack items="flex-start">
              <FooterTitle>Overview</FooterTitle>
            </YStack>
            <YStack gap="$2" items="flex-start">
              {MENU_ITEMS.map((item) => (
                <XStack key={item.id} self="flex-start">
                  <Anchor
                    href={`#${item.id}`}
                    onPress={(event) => {
                      event.preventDefault();
                      scrollToSection(item.id);
                    }}
                    textDecorationLine="none"
                    opacity={0.9}
                    hoverStyle={{ opacity: 1 }}
                    pressStyle={{ opacity: 0.75 }}
                    py="$1"
                    pl={item.level === 0 ? 0 : item.level === 1 ? "$3" : "$5"}
                  >
                    <Text
                      fontFamily="$body"
                      fontSize={item.level === 0 ? "$4" : "$3"}
                      color="$color12"
                    >
                      {item.label}
                    </Text>
                  </Anchor>
                </XStack>
              ))}
            </YStack>
          </YStack>

          <YStack
              gap="$4"
              items={footerAlign}
          >
            <YStack items="flex-start">
              <FooterTitle>Latest Version</FooterTitle>
            </YStack>
            <YStack gap="$1" items="flex-start">
              <XStack self="flex-start">
                <Anchor
                    href={`/`}
                    onPress={(event) => {
                      event.preventDefault();
                      onSelectLatestVersion();
                    }}
                    textDecorationLine="none"
                    opacity={0.9}
                    hoverStyle={{ opacity: 1 }}
                    pressStyle={{ opacity: 0.75 }}
                >
                  <Text
                      fontFamily="$body"
                      fontSize="$4"
                      color="$color12"
                  >
                    {latestVersion}
                  </Text>
                </Anchor>
              </XStack>
            </YStack>
          </YStack>

          <YStack
            gap="$4"
            items={footerAlign}
          >
            <YStack items="flex-start">
              <FooterTitle>Links</FooterTitle>
            </YStack>
            <YStack gap="$2" items="flex-start">
              {EXTERNAL_LINKS.map((item) => (
                <XStack key={item.href} self="flex-start" py="$1">
                  <Anchor
                    href={item.href}
                    target="_blank"
                    rel="noreferrer"
                    textDecorationLine="none"
                    opacity={0.9}
                    hoverStyle={{ opacity: 1 }}
                  >
                    <XStack items="center" gap="$2">
                    <Text
                      fontFamily="$body"
                      fontSize="$4"
                      color="$color12"
                    >
                      {item.label}
                    </Text>
                    <ExternalLink size={14} />
                    </XStack>
                  </Anchor>
                </XStack>
              ))}
            </YStack>
          </YStack>
        </YStack>
      </YStack>
    </YStack>
  );
}
