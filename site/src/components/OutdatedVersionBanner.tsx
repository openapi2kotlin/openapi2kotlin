import {AlertTriangle} from "lucide-react";
import {Link as RouterLink} from "react-router-dom";
import {Text, useTheme, XStack, YStack} from "tamagui";

export default function OutdatedVersionBanner() {
  const theme = useTheme();
  const bannerColor = theme.color8?.val;

  return (
    <YStack bg="$color2" borderWidth={1} borderColor="$color6" rounded="$5" px="$4" py="$5" mt="$3" mb="$3">
      <XStack items="center" justify="flex-start" gap="$3">
        <YStack pt={2}>
          <AlertTriangle size={14} color={bannerColor} />
        </YStack>
        <Text color="$color8" fontSize="$4" lineHeight="$3" fontWeight="700">
          You are viewing an older version of the documentation. For the latest features and updates, see the current version{" "}
          <Text asChild color="$color8" textDecorationLine="underline" fontWeight="700">
            <RouterLink to="/">here</RouterLink>
          </Text>
          .
        </Text>
      </XStack>
    </YStack>
  );
}
