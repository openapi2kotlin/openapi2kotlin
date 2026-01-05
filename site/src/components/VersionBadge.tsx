import {Anchor} from "tamagui";
import ButtonWithTooltip from "./ButtonWithTooltip.tsx";
import {Tag} from "lucide-react";

const MAVEN_URL =
    "https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin";

type Props = {
  version: string;
};

export default function VersionBadge({ version }: Props) {
  return (
      <Anchor href={MAVEN_URL} target="_blank" rel="noreferrer">
        <ButtonWithTooltip
            theme="green"
            size="$1"
            width={100}
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
          {version}
        </ButtonWithTooltip>
      </Anchor>
  );
}
