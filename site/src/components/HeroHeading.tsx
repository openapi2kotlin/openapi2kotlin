export function HeroHeading() {
  const WIDTH = 980;
  const HEIGHT = 320;

  return (
      <svg
          viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
          width="100%"
          style={{
            maxWidth: 980,
            height: "auto",
            display: "block",
          }}
      >
        <defs>
          <linearGradient id="accent" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#7F52FF"/>
            <stop offset="100%" stopColor="#E24462"/>
          </linearGradient>
        </defs>

        <rect width="980" height="320" fill="transparent"/>
        <text
            x="490"
            y="150"
            textAnchor="middle"
            fontFamily="Inter, ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial"
            fontSize="140"
            fontWeight="800"
            letterSpacing="-1"
            fill="currentColor"
        >
          <tspan>Open</tspan>
          <tspan fontWeight="900">API</tspan>
          <tspan
              baseline-shift="super"
              dy="-35"
              dx="5"
              fontSize="40"
              fontWeight="900"
              fill="url(#accent)"
          >2
          </tspan>
        </text>
        <text
            x="490"
            y="270"
            textAnchor="middle"
            fontFamily="Inter, ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial"
            fontSize="120"
            fontWeight="900"
            letterSpacing="-1"
            fill="currentColor"
        >
          Kotlin
        </text>
      </svg>
  )
}
