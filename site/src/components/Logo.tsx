import styles from "./Logo.module.css";
import {Anchor} from "tamagui";

export default function Logo() {
  const WIDTH = 500;
  const HEIGHT = 500;
  const ASPECT = HEIGHT / WIDTH;
  const SIZE = 32;

  return (
      <Anchor href="/" display="inline-flex" aria-label="OpenAPI 2 Kotlin Home">
        <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} width={SIZE} height={SIZE ? SIZE * ASPECT : undefined}
             className={styles.logo}>
          <g className="drawing-elements">
            <rect x="100" y="100" width="100" height="100" fill="#000000" stroke="" stroke-width="0" opacity="1"
                  id="rect-1" className=""/>
            <rect x="300" y="100" width="100" height="100" fill="#000000" stroke="" stroke-width="0" opacity="1"
                  id="rect-2" className=""/>
            <rect x="200" y="200" width="100" height="100" fill="#000000" stroke="" stroke-width="0" opacity="1"
                  id="rect-3" className=""/>
            <rect x="300" y="300" width="100" height="100" fill="#000000" stroke="" stroke-width="0" opacity="1"
                  id="rect-4" className="" />
            <rect x="100" y="300" width="100" height="100" fill="#000000" stroke="" stroke-width="0" opacity="1"
                  id="rect-5" className=""/>
            <rect x="100" y="200" width="100" height="100" fill="#000000" stroke="" stroke-width="0" opacity="1"
                  id="rect-6" className=""/>
          </g>
        </svg>
      </Anchor>
  );
}