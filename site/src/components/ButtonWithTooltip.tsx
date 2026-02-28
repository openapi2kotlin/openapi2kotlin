import {Button, type ButtonProps, Text, Tooltip, useTheme} from "tamagui";
import type { ComponentProps } from "react";
import type {LucideIcon} from "lucide-react";

type ButtonTextProps = {
  fontFamily?: ComponentProps<typeof Text>["fontFamily"];
  fontSize?: ComponentProps<typeof Text>["fontSize"];
  fontWeight?: ComponentProps<typeof Text>["fontWeight"];
};

export interface ButtonWithTooltipProps extends Omit<ButtonProps, keyof ButtonTextProps>, ButtonTextProps {
  tooltip?: string;
  icon: LucideIcon;
}

export default function ButtonWithTooltip(
    {
      size = "$2",
      tooltip,
      icon: Icon,
      fontFamily,
      fontSize,
      fontWeight,
      children,
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
      >
        <Button.Text
            fontFamily={fontFamily}
            fontSize={fontSize}
            fontWeight={fontWeight}
        >
          {children}
        </Button.Text>
      </Button>
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
            transition={[
              'medium',
              {
                opacity: {
                  overshootClamping: true,
                },
              },
            ]}
        >
          <Text fontSize="$2" whiteSpace="nowrap">
            {tooltip}
          </Text>
          <Tooltip.Arrow/>
        </Tooltip.Content>
      </Tooltip>
  );
}
