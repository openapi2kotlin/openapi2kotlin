import "./App.css";
import {Stack, Text, XStack} from "tamagui";
import DockNav from "./components/DockNav";
import CodeBlock from "./components/CodeBlock";
import {AmbientBackground} from "./components/AmbientBackground.tsx";
import {HeroAmbient} from "./components/HeroAmbient.tsx";
import ConfigOptionsTable from "./components/ConfigOptionsTable.tsx";
import {HeroHeading} from "./components/HeroHeading.tsx";
import {Link} from "lucide-react";

const TOML = `[versions]
openapi2kotlin = "0.10.0"

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
    property: "inputSpec =",
    description: "Path to OpenAPI YAML or JSON specification",
    example: `"$projectDir/src/main/resources/openapi.yaml"`,
  },
  {
    property: "outputDir =",
    description: "Root directory for generated Kotlin sources",
    example: `layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path`,
  },
  {
    property: "model { packageName = }",
    description: "Package name for generated model classes",
    example: `"dev.openapi2kotlin.model"`,
  },
];

export default function App() {
  return (
      <AmbientBackground>
        <DockNav/>

        <Stack minH="80vh" flex={1} justify="center" items="center">
          <HeroAmbient/>
          <HeroHeading/>
        </Stack>
        <Stack
            maxW={980}
            width="100%"
            mx="auto"
            px="$4"
            pt={96}
            pb="$7"
            gap="$6"
        >
          {/*<Stack gap="$2">*/}
          {/*  <Text fontFamily="$heading" fontSize="$10" fontWeight="800">*/}
          {/*    openapi2kotlin*/}
          {/*  </Text>*/}

          {/*  <Text fontSize="$4" opacity={0.85}>*/}
          {/*    Gradle plugin for generating Kotlin sources from an OpenAPI specification, engineered to handle*/}
          {/*    complex polymorphism including <Text fontFamily="$mono">oneOf</Text> and{" "}*/}
          {/*    <Text fontFamily="$mono">allOf</Text>.*/}
          {/*  </Text>*/}
          {/*</Stack>*/}

          {/*<Separator/>*/}

          <Stack gap="$3">
            <Text fontFamily="$heading" fontSize="$7" fontWeight="800">
              Installation &amp; Usage
            </Text>

            <Text fontSize="$4" opacity={0.85}>
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

            <Stack gap="$4">
              <CodeBlock title="libs.versions.toml" code={TOML}/>
              <CodeBlock title="build.gradle.kts" code={GRADLE}/>
            </Stack>

            <ConfigOptionsTable rows={CONFIG_ROWS}/>
          </Stack>
        </Stack>
      </AmbientBackground>
  );
}