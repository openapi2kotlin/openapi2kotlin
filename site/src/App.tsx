import "./App.css";
import {Stack, Text, XStack} from "tamagui";
import DockNav from "./components/DockNav";
import CodeBlock from "./components/CodeBlock";
import {AmbientBackground} from "./components/AmbientBackground.tsx";
import {HeroAmbient} from "./components/HeroAmbient.tsx";
import ConfigOptionsTable from "./components/ConfigOptionsTable.tsx";
import {HeroHeading} from "./components/HeroHeading.tsx";
import {Link} from "lucide-react";
import {useMedia} from "@tamagui/core";

const LATEST_STABLE_RELEASE_VERSION = import.meta.env.VITE_LATEST_STABLE_RELEASE_VERSION as string

const TOML = `[versions]
openapi2kotlin = "${LATEST_STABLE_RELEASE_VERSION}"

[plugins]
openapi2kotlin = { id = "dev.openapi2kotlin", version.ref = "openapi2kotlin" }`;

const GRADLE = `plugins {
    alias(libs.plugins.openapi2kotlin)
}

openapi2kotlin {
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "dev.openapi2kotlin.model"
    }
}`;

const CONFIG_ROWS = [
  {
    property: "inputSpec",
    description: "Path to OpenAPI YAML or JSON specification",
    example: `"$projectDir/src/main/resources/openapi.yaml"`,
  },
  {
    property: "outputDir",
    description: "Root directory for generated Kotlin sources",
    example: `layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path`,
  },
  {
    property: "model.packageName",
    description: "Package name for generated model classes",
    example: `"dev.openapi2kotlin.model"`,
  },
];

export default function App() {
  const media = useMedia()

  return (
      <AmbientBackground>
        <HeroAmbient />
        <DockNav/>
        <Stack
            maxW={980}
            width="100%"
            mx="auto"
            px="$4"
            pb="$7"
            gap="$6"
        >
          <Stack minH="80vh" maxW={980} flex={1} justify="center" items="center" position="relative" px={media.maxXs ? 0 : 100}>
            <HeroHeading />
          </Stack>

          <Stack gap="$3">
            <Text fontFamily="$heading" fontSize="$7" fontWeight="800">
              Installation &amp; Usage
            </Text>

            <Text fontSize="$4" color="$color11" opacity={0.85}>
              Copy and paste the snippet below to apply the plugin. The OpenAPI specification will then be processed as part of the build, producing the generated Kotlin sources.
            </Text>

            <Stack gap="$4">
              <CodeBlock title="libs.versions.toml" code={TOML}/>
              <CodeBlock title="build.gradle.kts" code={GRADLE}/>
            </Stack>

            <Text fontSize="$4" opacity={0.85} color="$color11">
              The plugin is published to{" "}
              <Text asChild>
                <a
                    href="https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin"
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{textDecoration: "none"}}
                >
                  <XStack
                      asChild
                      display="inline-flex"
                      items="center"
                      gap="$2"
                      px="$2"
                      py="$1"
                      rounded="$3"
                      bg="$pink2"
                      hoverStyle={{bg: "$pink4"}}
                      pressStyle={{bg: "$pink5"}}
                  >
                    <span>
                      <Text fontWeight="600" color="$color12" fontFamily="$mono">
                        Maven Central
                      </Text>
                      <Link size={14}/>
                    </span>
                  </XStack>
                </a>
              </Text>
              .
            </Text>

            <ConfigOptionsTable rows={CONFIG_ROWS}/>
          </Stack>
        </Stack>
      </AmbientBackground>
  );
}