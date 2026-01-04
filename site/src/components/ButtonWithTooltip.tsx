import {Button, type ButtonProps, Text, Tooltip, useTheme} from "tamagui";
import type {LucideIcon} from "lucide-react";

export interface ButtonWithTooltipProps extends ButtonProps {
  tooltip?: string;
  icon: LucideIcon;
}

export default function ButtonWithTooltip(
    {
      size = "$2",
      tooltip,
      icon: Icon,
      ...props
    }: ButtonWithTooltipProps
) {
  const theme = useTheme();
  const iconColor = theme.color11?.val;

  const button = (
      <Button
          {...props}
          size={size}
          borderColor="transparent"
          aria-label={tooltip}
          icon={(props) => <Icon {...props} color={iconColor}/>}
      />
  );

  if (!tooltip) {
    return button;
  }

  return (
      <Tooltip delay={200} placement="bottom">
        <Tooltip.Trigger asChild>
          {button}
        </Tooltip.Trigger>

        <Tooltip.Content
            bg="$color1"
            p="$2"
            z={1100}
            enterStyle={{ x: 0, y: -5, opacity: 0, scale: 0.9 }}
            exitStyle={{ x: 0, y: -5, opacity: 0, scale: 0.9 }}
            scale={1}
            x={0}
            y={0}
            opacity={1}
            animation={[
              'bouncier',
              {
                opacity: {
                  overshootClamping: true,
                },
              },
            ]}
        >
          <Text fontSize="$2">{tooltip}</Text>
          <Tooltip.Arrow/>
        </Tooltip.Content>
      </Tooltip>
  );
}
