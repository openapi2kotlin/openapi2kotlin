import { useToastController } from "@tamagui/toast";

type BaseOptions = Parameters<ReturnType<typeof useToastController>["show"]>[1];

type Options = Omit<BaseOptions, "theme" | "themeInverse">;

export function useAppToast() {
  const toast = useToastController();

  function success(title: string, options: Options = {}) {
    toast.show(title, {
      burnt: "true",
      duration: 2000,
      themeInverse: true,
      ...options,
    });
  }

  function error(title: string, options: Options = {}) {
    toast.show(title, {
      burnt: "true",
      duration: 2000,
      theme: "error",
      ...options,
    });
  }

  return {
    ...toast,
    success,
    error,
  };
}
