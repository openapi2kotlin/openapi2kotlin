import type { VersionDocs } from '../model/version-docs'

const modules = import.meta.glob('../../docs/*.json', { eager: true })

function toVersionDocs(mod: unknown): VersionDocs {
  const m = mod as { default?: VersionDocs }
  return (m.default ?? mod) as VersionDocs
}

export const VERSION_DOCS_LIST: VersionDocs[] = Object.values(modules)
  .map(toVersionDocs)
  .filter((d) => /^\d+\.\d+\.\d+([-.].+)?$/.test(d.version))
  .sort((a, b) => compareVersionsDesc(a.version, b.version))

export const VERSION_DOCS_BY_VERSION: Record<string, VersionDocs> = Object.fromEntries(
  VERSION_DOCS_LIST.map((d) => [d.version, d]),
)

function compareVersionsDesc(a: string, b: string): number {
  const pa = a.split(/[.-]/)
  const pb = b.split(/[.-]/)
  const n = Math.max(pa.length, pb.length)
  for (let i = 0; i < n; i++) {
    const xa = pa[i] ?? '0'
    const xb = pb[i] ?? '0'
    const na = Number(xa)
    const nb = Number(xb)
    const bothNum = !Number.isNaN(na) && !Number.isNaN(nb)
    if (bothNum && na !== nb) return nb - na
    if (!bothNum && xa !== xb) return xb.localeCompare(xa)
  }
  return 0
}
