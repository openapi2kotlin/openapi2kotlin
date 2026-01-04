import {Anchor} from "tamagui";
import {Github} from "lucide-react";
import ChromelessCircularButton from "./ChromelessCircularButton.tsx";

const GITHUB_URL =
    "https://github.com/openapi2kotlin/openapi2kotlin";

export default function GitHubLink() {
  return (
      <Anchor href={GITHUB_URL} target="_blank" rel="noreferrer">
        <ChromelessCircularButton
            size="$4"
            width={100}
            rounded="$10"
            fontFamily="$mono"
            fontSize="$3"
            fontWeight="700"
            icon={Github}
            tooltip="GitHub"
        >
        </ChromelessCircularButton>
      </Anchor>
  );
}
