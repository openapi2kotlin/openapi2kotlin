import {Toast, useToastState} from "@tamagui/toast";
import {XStack, YStack} from "tamagui";
import {X} from "lucide-react";
import ChromelessCircularButton from "./ChromelessCircularButton";

export default function AppToast() {
  const currentToast = useToastState()

  if (!currentToast || currentToast.ishandlednatively) {
    return null
  }

  return (
      <Toast
          {...currentToast}
          animation="200ms"
          key={currentToast.id}
          duration={currentToast.duration}
          enterStyle={{opacity: 0, transform: [{translateY: 100}]}}
          exitStyle={{opacity: 0, transform: [{translateY: 100}]}}
          transform={[{translateY: 0}]}
          opacity={1}
          scale={1}
          viewportName={currentToast.viewportName}
      >
        <XStack items="center" justify="space-between" gap="$2">
          <YStack flex={1}>
            <Toast.Title>{currentToast.title}</Toast.Title>

            {!!currentToast.message && (
                <Toast.Description>{currentToast.message}</Toast.Description>
            )}
          </YStack>

          {/* Custom close button */}
          <Toast.Close asChild>
            <ChromelessCircularButton
                icon={X}
            />
          </Toast.Close>
        </XStack>
      </Toast>
  )
}