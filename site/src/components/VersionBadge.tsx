import {Anchor} from "tamagui";
import ButtonWithTooltip from "./ButtonWithTooltip.tsx";
import {Tag} from "lucide-react";

const MAVEN_URL =
    "https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin";

const LATEST_STABLE_RELEASE_VERSION = __LATEST_STABLE_RELEASE_VERSION__;

export default function VersionBadge() {
  return (
      <Anchor href={MAVEN_URL} target="_blank" rel="noreferrer">
        <ButtonWithTooltip
            theme="green"
            rounded="$10"
            fontFamily="$mono"
            fontSize="$3"
            fontWeight="700"
            pressStyle={{
              scale: 0.96,
            }}
            tooltip="Latest stable release"
            icon={Tag}
        >
          {LATEST_STABLE_RELEASE_VERSION}
        </ButtonWithTooltip>
      </Anchor>
  );
}
