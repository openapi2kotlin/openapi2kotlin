import { useEffect, useMemo, useRef, useState } from "react";
import { Button, type TamaguiElement, Text, YStack } from "tamagui";

type MenuItem = {
  id: string;
  label: string;
  level?: 0 | 1 | 2;
};

const MENU_ITEMS: MenuItem[] = [
  { id: "installation", label: "Installation", level: 0 },
  { id: "api-reference", label: "API Reference", level: 0 },
  { id: "openapi2kotlin", label: "openapi2kotlin", level: 1 },
  { id: "openapi2kotlin-model", label: "model", level: 2 },
  { id: "openapi2kotlin-client", label: "client", level: 2 },
  { id: "openapi2kotlin-server", label: "server", level: 2 },
];

const NAV_OFFSET = 90;
const ACTIVE_MARKER_OFFSET = NAV_OFFSET;
const CLICK_SCROLL_MUTE_MS = 2000;

export default function ContentsMenu() {
  const [activeId, setActiveId] = useState<string>(MENU_ITEMS[0].id);
  const [sliderTop, setSliderTop] = useState(0);
  const [railTop, setRailTop] = useState(0);
  const [railHeight, setRailHeight] = useState(0);
  const itemRefs = useRef<Record<string, TamaguiElement | null>>({});
  const muteScrollSyncRef = useRef(false);
  const unmuteTimerRef = useRef<number | null>(null);

  useEffect(() => {
    const updateActive = () => {
      const marker = window.innerHeight * 0.5;
      const candidates = MENU_ITEMS
        .map((item) => {
          const el = document.getElementById(item.id);
          if (!el) return null;
          return { id: item.id, top: el.getBoundingClientRect().top };
        })
        .filter((v): v is { id: string; top: number } => v !== null);

      if (candidates.length === 0) return;

      const passed = candidates.filter((c) => c.top <= marker);
      if (passed.length > 0) {
        setActiveId(passed[passed.length - 1].id);
        return;
      }
      setActiveId(candidates[0].id);
    };

    let rafId = 0;
    const onScroll = () => {
      if (muteScrollSyncRef.current) return;
      if (rafId) return;
      rafId = window.requestAnimationFrame(() => {
        rafId = 0;
        updateActive();
      });
    };

    const scrollTargets: EventTarget[] = [
      window,
      document,
      document.documentElement,
      document.body,
      document.scrollingElement ?? document.documentElement,
    ].filter((target, index, arr) => arr.indexOf(target) === index);

    updateActive();
    for (const target of scrollTargets) {
      target.addEventListener("scroll", onScroll, { passive: true });
    }
    window.addEventListener("resize", updateActive);

    const unmuteOnUserIntent = () => {
      if (!muteScrollSyncRef.current) return;
      muteScrollSyncRef.current = false;
      if (unmuteTimerRef.current !== null) {
        window.clearTimeout(unmuteTimerRef.current);
        unmuteTimerRef.current = null;
      }
      updateActive();
    };
    window.addEventListener("wheel", unmuteOnUserIntent, { passive: true });
    window.addEventListener("touchmove", unmuteOnUserIntent, { passive: true });
    window.addEventListener("keydown", unmuteOnUserIntent);

    return () => {
      if (rafId) {
        window.cancelAnimationFrame(rafId);
      }
      for (const target of scrollTargets) {
        target.removeEventListener("scroll", onScroll);
      }
      window.removeEventListener("resize", updateActive);
      window.removeEventListener("wheel", unmuteOnUserIntent);
      window.removeEventListener("touchmove", unmuteOnUserIntent);
      window.removeEventListener("keydown", unmuteOnUserIntent);
    };
  }, []);

  useEffect(() => {
    return () => {
      if (unmuteTimerRef.current !== null) {
        window.clearTimeout(unmuteTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const scrollMarginTop = `${NAV_OFFSET}px`;
    for (const item of MENU_ITEMS) {
      const el = document.getElementById(item.id);
      if (!el) continue;
      el.style.scrollMarginTop = scrollMarginTop;
    }
  }, []);

  useEffect(() => {
    const activeEl = itemRefs.current[activeId];
    if (!activeEl) return;
    setSliderTop(activeEl.offsetTop + activeEl.offsetHeight / 2 - 13);
  }, [activeId]);

  useEffect(() => {
    const updateRail = () => {
      const firstId = MENU_ITEMS[0].id;
      const lastId = MENU_ITEMS[MENU_ITEMS.length - 1].id;
      const firstEl = itemRefs.current[firstId];
      const lastEl = itemRefs.current[lastId];
      if (!firstEl || !lastEl) return;

      const firstCenter = firstEl.offsetTop + firstEl.offsetHeight / 2;
      const lastCenter = lastEl.offsetTop + lastEl.offsetHeight / 2;
      setRailTop(firstCenter);
      setRailHeight(Math.max(0, lastCenter - firstCenter));
    };

    updateRail();
    window.addEventListener("resize", updateRail);
    return () => {
      window.removeEventListener("resize", updateRail);
    };
  }, [activeId]);

  const onNavigate = (id: string) => {
    const el = document.getElementById(id);
    if (!el) return;
    muteScrollSyncRef.current = true;
    if (unmuteTimerRef.current !== null) {
      window.clearTimeout(unmuteTimerRef.current);
    }
    setActiveId(id);
    const bodyIsScrollable =
      document.body.scrollHeight > document.body.clientHeight &&
      getComputedStyle(document.body).overflowY !== "visible";
    const scrollContainer: Window | HTMLElement =
      bodyIsScrollable
        ? document.body
        : ((document.scrollingElement as HTMLElement | null) ?? window);
    const scrollTop =
      scrollContainer === window
        ? window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0
        : (scrollContainer as HTMLElement).scrollTop;
    const targetTop = el.getBoundingClientRect().top + scrollTop - ACTIVE_MARKER_OFFSET;
    const nextTop = Math.max(0, targetTop);
    if (scrollContainer === window) {
      window.scrollTo({ top: nextTop, behavior: "smooth" });
    } else {
      scrollContainer.scrollTo({ top: nextTop, behavior: "smooth" });
    }
    window.history.replaceState(null, "", `#${id}`);
    unmuteTimerRef.current = window.setTimeout(() => {
      muteScrollSyncRef.current = false;
      unmuteTimerRef.current = null;
    }, CLICK_SCROLL_MUTE_MS);
  };

  const minMenuHeight = useMemo(() => MENU_ITEMS.length * 32, []);

  return (
    <YStack
      gap="$2"
      pointerEvents="auto"
    >
      <Text fontSize="$2" opacity={0.65} fontWeight="700">
        Contents
      </Text>

      <YStack position="relative" pl="$4" minH={minMenuHeight}>
        <YStack
          position="absolute"
          l={6}
          t={railTop}
          width={2}
          height={railHeight}
          bg="$color6"
          opacity={0.45}
          rounded="$10"
        />
        <YStack
          position="absolute"
          l={5}
          t={sliderTop}
          width={4}
          height={24}
          rounded="$10"
          bg="$color11"
          transition="mediumLessBouncy"
        />

        <YStack gap="$1">
          {MENU_ITEMS.map((item) => {
            const isActive = activeId === item.id;
            const level = item.level ?? 0;
            const leftPad = level === 0 ? "$2" : level === 1 ? "$5" : "$7";
            return (
              <Button
                key={item.id}
                ref={(el) => {
                  itemRefs.current[item.id] = el;
                }}
                onClick={() => onNavigate(item.id)}
                justify="flex-start"
                size="$2"
                bg="transparent"
                borderWidth={0}
                opacity={0.8}
                hoverStyle={{
                  bg: "transparent",
                  opacity: 1,
                }}
                focusStyle={{
                  bg: "transparent",
                }}
                pressStyle={{
                  bg: "transparent",
                }}
                pl={leftPad}
                rounded="$3"
              >
                <Button.Text
                  color={isActive ? "$color11" : "$color10"}
                  fontSize={level === 0 ? "$4" : "$3"}
                  fontWeight={isActive ? 700 : 500}
                >
                  {item.label}
                </Button.Text>
              </Button>
            );
          })}
        </YStack>
      </YStack>
    </YStack>
  );
}
