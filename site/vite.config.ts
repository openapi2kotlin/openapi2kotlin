import react from '@vitejs/plugin-react-swc'
import { tamaguiPlugin } from '@tamagui/vite-plugin'
import { dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
const siteDir = dirname(fileURLToPath(import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, siteDir, '')
  return {
    envDir: siteDir,
    define: {
      __LATEST_STABLE_RELEASE_VERSION__: JSON.stringify(env.VITE_LATEST_STABLE_RELEASE_VERSION ?? ''),
    },
    plugins: [
      react(),
      tamaguiPlugin({
        config: 'tamagui/tamagui.config.ts',
        components: ['tamagui'],
        disableExtraction: true,
      }),
    ].filter(Boolean)
  }
})
