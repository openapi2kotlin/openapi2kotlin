import {themes} from './themes'
import {defaultConfig} from '@tamagui/config/v4'
import {createTamagui} from "tamagui";
import {fonts} from "./fonts";
import {animations} from "./animations";

export const config = createTamagui({
  ...defaultConfig,
  themes,
  fonts,
  animations,
})

export type AppConfig = typeof config

declare module 'tamagui' {
  interface TamaguiCustomConfig extends AppConfig {
  }
}