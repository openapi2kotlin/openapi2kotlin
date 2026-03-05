export type ConfigRow = {
  property: string
  description: string
  values: string
  required: boolean
  default: string
}

export type VersionDocs = {
  version: string
  generatedAt: string
  clientLibraries: string[]
  serverLibraries: string[]
  defaultClientLibrary: string
  defaultServerLibrary: string
  configRows: ConfigRow[]
  snippets: {
    client: Record<string, string>
    server: Record<string, string>
  }
}
