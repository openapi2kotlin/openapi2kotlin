import "./App.css";
import {useEffect, useLayoutEffect, useMemo, useState} from "react";
import {H2, Text, Theme, XStack, YStack} from "tamagui";
import {AlertTriangle, ExternalLink} from "lucide-react";
import DockNav from "./components/DockNav";
import CodeBlock from "./components/CodeBlock";
import {AmbientBackground} from "./components/AmbientBackground.tsx";
import {HeroAmbient} from "./components/HeroAmbient.tsx";
import ConfigOptionsTable from "./components/ConfigOptionsTable.tsx";
import ContentsMenu from "./components/ContentsMenu.tsx";
import {HeroHeading} from "./components/HeroHeading.tsx";
import {useMedia} from "@tamagui/core";
import SegmentedControl from "./components/SegmentedControl.tsx";
import {VERSION_DOCS_BY_VERSION, VERSION_DOCS_LIST} from "./service/version-docs-registry";
import {Link as RouterLink, useLocation, useNavigate, useParams} from "react-router-dom";

type ApiTarget = "Client" | "Server";

const CONTENT_WIDTH = 980;
const SIDE_COLUMN_WIDTH = 280;

function getScrollContainer(): Window | HTMLElement {
  const bodyOverflowY = getComputedStyle(document.body).overflowY;
  const bodyIsScrollable =
    document.body.scrollHeight > document.body.clientHeight &&
    bodyOverflowY !== "visible" &&
    bodyOverflowY !== "hidden";

  if (bodyIsScrollable) {
    return document.body;
  }

  return (document.scrollingElement as HTMLElement | null) ?? window;
}

function getCurrentScrollTop(scrollContainer: Window | HTMLElement) {
  if (scrollContainer === window) {
    return window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0;
  }
  return (scrollContainer as HTMLElement).scrollTop;
}

function scrollToY(top: number) {
  const scrollContainer = getScrollContainer();
  if (scrollContainer === window) {
    window.scrollTo({ top, behavior: "auto" });
    return;
  }
  scrollContainer.scrollTo({ top, behavior: "auto" });
}

function scrollPageToTop() {
  scrollToY(0);
}

function scrollToHash(hash: string) {
  if (!hash) return false;
  const targetId = decodeURIComponent(hash.replace(/^#/, ""));
  if (!targetId) return false;
  const el = document.getElementById(targetId);
  if (!el) return false;
  const scrollContainer = getScrollContainer();
  const scrollTop = getCurrentScrollTop(scrollContainer);
  const top = el.getBoundingClientRect().top + scrollTop - 90;
  scrollToY(Math.max(0, top));
  return true;
}

export default function App() {
  const media = useMedia();
  const navigate = useNavigate();
  const location = useLocation();
  const { version } = useParams();
  const latestVersion = VERSION_DOCS_LIST[0]?.version ?? __LATEST_STABLE_RELEASE_VERSION__;
  const selectedVersion =
    (version && VERSION_DOCS_BY_VERSION[version]?.version) ? version : latestVersion;
  const docs = VERSION_DOCS_BY_VERSION[selectedVersion] ?? VERSION_DOCS_BY_VERSION[latestVersion];

  useLayoutEffect(() => {
    if (!location.hash) {
      scrollPageToTop();
      return;
    }

    let retries = 0;
    let timeoutId = 0;
    const tryScroll = () => {
      if (scrollToHash(location.hash)) return;
      if (retries >= 40) return;
      retries += 1;
      timeoutId = window.setTimeout(() => {
        window.requestAnimationFrame(tryScroll);
      }, 25);
    };
    window.requestAnimationFrame(tryScroll);
    return () => {
      if (timeoutId) {
        window.clearTimeout(timeoutId);
      }
    };
  }, [location.pathname, location.hash]);

  const navigateToVersion = (nextVersion: string) => {
    if (nextVersion === latestVersion) {
      navigate({ pathname: "/", search: "", hash: "" });
    } else {
      navigate({ pathname: `/${nextVersion}`, search: "", hash: "" });
    }
  };

  return (
    <AppContent
      key={docs.version}
      docs={docs}
      selectedVersion={selectedVersion}
      latestVersion={latestVersion}
      onSelectVersion={navigateToVersion}
      mediaMaxXs={media.maxXs}
      mediaMaxMd={media.maxMd}
    />
  );
}

function AppContent({
  docs,
  selectedVersion,
  latestVersion,
  onSelectVersion,
  mediaMaxXs,
  mediaMaxMd,
}: {
  docs: (typeof VERSION_DOCS_LIST)[number];
  selectedVersion: string;
  latestVersion: string;
  onSelectVersion: (nextVersion: string) => void;
  mediaMaxXs: boolean;
  mediaMaxMd: boolean;
}) {

  const [apiTarget, setApiTarget] = useState<ApiTarget>("Client");
  const [clientLibrary, setClientLibrary] = useState<string>(docs.defaultClientLibrary);
  const [serverLibrary, setServerLibrary] = useState<string>(docs.defaultServerLibrary);
  const [isUnder1100, setIsUnder1100] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    return window.innerWidth < 1100;
  });
  const effectiveClientLibrary = docs.clientLibraries.includes(clientLibrary)
    ? clientLibrary
    : docs.defaultClientLibrary;
  const effectiveServerLibrary = docs.serverLibraries.includes(serverLibrary)
    ? serverLibrary
    : docs.defaultServerLibrary;
  const hideSideColumns = mediaMaxMd || isUnder1100;

  useEffect(() => {
    const onResize = () => {
      setIsUnder1100(window.innerWidth < 1100);
    };
    onResize();
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  const onApiTargetChange = (next: ApiTarget) => {
    setApiTarget(next);
    if (next === "Client") {
      setClientLibrary(docs.defaultClientLibrary);
      return;
    }
    setServerLibrary(docs.defaultServerLibrary);
  };

  const apiSnippet = useMemo(() => {
    if (apiTarget === "Client") {
      return docs.snippets.client[effectiveClientLibrary] ?? "";
    }
    return docs.snippets.server[effectiveServerLibrary] ?? "";
  }, [apiTarget, docs.snippets.client, docs.snippets.server, effectiveClientLibrary, effectiveServerLibrary]);

  const tomlSnippet = useMemo(
    () => `[versions]
openapi2kotlin = "${selectedVersion}"

[plugins]
openapi2kotlin = { id = "dev.openapi2kotlin", version.ref = "openapi2kotlin" }`,
    [selectedVersion],
  );

  const gradleSnippet = useMemo(
    () => `plugins {
    alias(libs.plugins.openapi2kotlin)
}

openapi2kotlin {
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "dev.openapi2kotlin.model"
    }
${apiSnippet.trimEnd()}
}`,
    [apiSnippet],
  );

  return (
    <AmbientBackground>
      <HeroAmbient />
      <DockNav
        selectedVersion={selectedVersion}
        availableVersions={VERSION_DOCS_LIST.map((d) => d.version)}
        latestVersion={latestVersion}
        onSelectVersion={onSelectVersion}
      />
      <YStack width="100%" px="$4" pb="$7" gap="$6">
        <YStack
          minH="80vh"
          maxW={CONTENT_WIDTH}
          width="100%"
          mx="auto"
          flex={1}
          justify="center"
          items="center"
          position="relative"
          px={mediaMaxXs ? 0 : 100}
        >
          <HeroHeading />
        </YStack>

        <XStack
          width="100%"
          maxW={hideSideColumns ? CONTENT_WIDTH : CONTENT_WIDTH + SIDE_COLUMN_WIDTH * 2}
          mx="auto"
          items="flex-start"
        >
          <YStack
            display={hideSideColumns ? "none" : "flex"}
            width={SIDE_COLUMN_WIDTH}
            minW={SIDE_COLUMN_WIDTH}
            style={{ flexShrink: 0 }}
          >
            {/* Left navigation */}
          </YStack>
          <YStack
            flex={1}
            width={mediaMaxMd ? "100%" : undefined}
            maxW={CONTENT_WIDTH}
            minW={0}
          >
            {/* Content */}
            <YStack gap="$3" position="relative">
              <H2 id="installation">Installation</H2>

              <Text fontSize="$4" color="$color11" opacity={0.85}>
                Choose which client or server you’d like to generate, then copy the snippets below.
              </Text>

              {selectedVersion !== latestVersion ? (
                  <Theme name="yellow">
                    <YStack bg="$color2" borderWidth={1} borderColor="$color6" rounded="$5" px="$4" py="$3" mt="$3" mb="$3">
                      <XStack items="flex-start" gap="$2">
                        <YStack pt={2}>
                          <AlertTriangle size={14} />
                        </YStack>
                        <Text color="$color12" fontSize="$3" lineHeight="$3">
                          You are viewing an older version of the documentation. For the latest features and updates, see the current version{" "}
                          <Text asChild color="$color12" textDecorationLine="underline" fontWeight="700">
                            <RouterLink to="/">here</RouterLink>
                          </Text>
                          .
                        </Text>
                      </XStack>
                    </YStack>
                  </Theme>
              ) : null}

              <YStack gap="$4">
                <Theme name="pink">
                  <YStack gap="$2">
                    <SegmentedControl
                        value={apiTarget}
                        onChange={onApiTargetChange}
                        options={[
                          { value: "Client", label: "Client" },
                          { value: "Server", label: "Server" },
                        ]}
                    />

                    {apiTarget === "Client" ? (
                        <SegmentedControl
                            value={effectiveClientLibrary}
                            onChange={setClientLibrary}
                            options={docs.clientLibraries.map((library) => ({
                              value: library,
                              label: library,
                              iconSrc: library === "Ktor" ? "/ktor.svg" : "/spring.svg",
                              iconAlt: `${library} logo`,
                              iconSize: library === "Ktor" ? 16 : 18,
                            }))}
                        />
                    ) : (
                        <SegmentedControl
                            value={effectiveServerLibrary}
                            onChange={setServerLibrary}
                            options={docs.serverLibraries.map((library) => ({
                              value: library,
                              label: library,
                              iconSrc: library === "Ktor" ? "/ktor.svg" : "/spring.svg",
                              iconAlt: `${library} logo`,
                              iconSize: library === "Ktor" ? 16 : 18,
                            }))}
                        />
                    )}
                  </YStack>
                </Theme>

                <CodeBlock title="libs.versions.toml" code={tomlSnippet} />

                <CodeBlock title="build.gradle.kts" code={gradleSnippet} />

                <Text fontSize="$4" opacity={0.85} color="$color11">
                  The plugin is published to{" "}
                  <Text asChild>
                    <a
                        href="https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin"
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{ textDecoration: "none" }}
                    >
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
                        <XStack>
                          <Text fontWeight="600" color="$color12" items="center"  fontFamily="$mono">
                            Maven Central <ExternalLink size={14} />
                          </Text>
                        </XStack>
                      </YStack>
                    </a>
                  </Text>
                  .
                </Text>
              </YStack>
            </YStack>


            <H2 id="api-reference" mt="$12" mb="$3">API Reference</H2>
            <Text fontSize="$4" color="$color11" opacity={0.85} mb="$8">
              Configuration options are grouped by scope so you can quickly find root, model, client, and server settings.
            </Text>
            <ConfigOptionsTable rows={docs.configRows} />
          </YStack>
          <YStack
            display={hideSideColumns ? "none" : "flex"}
            width={SIDE_COLUMN_WIDTH}
            minW={SIDE_COLUMN_WIDTH}
            style={{ flexShrink: 0 }}
            pl="$10"
            height="100%"
          >
            {/* Right navigation */}
            {!hideSideColumns ? (
                <YStack
                    pointerEvents="auto"
                    style={{ position: "sticky" }}
                    t="27vh"
                >
                  <ContentsMenu />
                </YStack>
            ) : null}
          </YStack>
        </XStack>
      </YStack>
    </AmbientBackground>
  );
}
