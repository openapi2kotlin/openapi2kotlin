import {Anchor} from "tamagui";

export default function Logo() {
  const WIDTH = 700;
  const HEIGHT = 700;
  const ASPECT = HEIGHT / WIDTH;
  const SIZE = 32;

  return (
      <Anchor href="/" display="inline-flex" aria-label="OpenAPI 2 Kotlin Home">
        <svg
            viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
            width={SIZE}
            height={SIZE ? SIZE * ASPECT : undefined}
        >
          <defs>
            <linearGradient
                id="kotlinGradient"
                gradientUnits="userSpaceOnUse"
                x1="0"
                y1="0"
                x2={WIDTH}
                y2={HEIGHT}
            >
              <stop offset="0%" stopColor="#7F52FF" />
              <stop offset="100%" stopColor="#E24462" />
            </linearGradient>
          </defs>

          <g className="drawing-elements">
            <rect x="100" y="300" width="100" height="100" fill="url(#kotlinGradient)" opacity="1" id="rect-1"/>
            <rect x="300" y="200" width="100" height="100" fill="url(#kotlinGradient)" opacity="1" id="rect-1"/>
            <rect x="500" y="200" width="100" height="100" fill="url(#kotlinGradient)" opacity="1" id="rect-2"/>
            <rect x="400" y="300" width="100" height="100" fill="url(#kotlinGradient)" opacity="1" id="rect-3"/>
            <rect x="500" y="400" width="100" height="100" fill="url(#kotlinGradient)" opacity="1" id="rect-4"/>
            <rect x="300" y="400" width="100" height="100" fill="url(#kotlinGradient)" opacity="1" id="rect-5"/>
            <rect x="300" y="300" width="100" height="100" fill="url(#kotlinGradient)" opacity="1" id="rect-6"/>
          </g>
        </svg>
      </Anchor>
  );
}
