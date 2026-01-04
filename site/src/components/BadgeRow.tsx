import { Anchor, Image, XStack } from "tamagui";

type Badge = {
  href: string;
  src: string;
  alt: string;
};

const badges: Badge[] = [
  {
    href: "https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin",
    src: "https://img.shields.io/maven-central/v/dev.openapi2kotlin/openapi2kotlin",
    alt: "Maven Central",
  },
  {
    href: "https://openapi2kotlin.dev/",
    src: "https://img.shields.io/badge/website-openapi2kotlin.dev-0b0b0b",
    alt: "Website",
  },
  {
    href: "https://www.apache.org/licenses/LICENSE-2.0",
    src: "https://img.shields.io/badge/license-Apache%202.0-blue.svg",
    alt: "License",
  },
];

export default function BadgeRow() {
  return (
      <XStack
          gap="$2"
          flexWrap="wrap"
          items="center"
          justify="center"
          width="100%"
      >
        {badges.map((b) => (
            <Anchor key={b.alt} href={b.href} target="_blank" rel="noreferrer">
              <Image
                  src={b.src}
                  alt={b.alt}
                  height={20}
                  width="auto"
                  resizeMode="contain"
              />
            </Anchor>
        ))}
      </XStack>
  );
}
