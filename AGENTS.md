# AGENTS

## Architecture
- `core` owns framework-agnostic generation logic and OpenAPI interpretation.
- `adapter` modules own framework-specific code generation and formatting details.
- New framework support should prefer adapter-only changes. Touch `usecase`, `configuration`, or `gradle-plugin` only when exposing a new public target.

## Generator conventions
- Clients generate framework-facing `ApiImpl` classes and keep transport construction outside generated code.
- Servers generate user-facing `Api` interfaces plus routing/bootstrap helpers.
- Framework-specific transport helpers such as `WithHttpInfo` belong in adapter modules.
- Reuse `apiContext`, model metadata, and naming/base-path behavior from core instead of re-deriving OpenAPI behavior in adapters.

## Framework extension rules
- Match existing product shape before inventing framework-specific patterns.
- Default serialization should follow established framework defaults unless there is a strong reason not to.
- Keep generated APIs useful for real applications: expose raw transport variants when the framework makes headers/status meaningful.
- Prefer standard framework primitives first; richer framework-specific DSLs can be a later option once the basic adapter is stable.

## Tests
- Every framework target must be covered by standalone fixtures under `test/`.
- Fixture matrix should include both `tmf620` and `petstore3` for client and server.
- Each standalone fixture must have:
  - its own Gradle wrapper/settings/build/catalog
  - `src/test/...Test.kt`
  - warnings as errors
  - participation in `./gradlew clean build all`

## Docs
- API Reference, snippets, and site segmented controls must be updated whenever a public generator target is added.
- Keep library ordering consistent across docs and UI.
- Versioned docs and README should be regenerated after public DSL changes.
