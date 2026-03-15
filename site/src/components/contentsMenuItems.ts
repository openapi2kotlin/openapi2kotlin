export type MenuItem = {
  id: string;
  label: string;
  level?: 0 | 1 | 2;
};

export const MENU_ITEMS: MenuItem[] = [
  { id: "installation", label: "Installation", level: 0 },
  { id: "under-the-hood", label: "Under the Hood", level: 0 },
  { id: "api-reference", label: "API Reference", level: 0 },
  { id: "openapi2kotlin", label: "Openapi2kotlin", level: 1 },
  { id: "openapi2kotlin-model", label: "Model", level: 2 },
  { id: "openapi2kotlin-client", label: "Client", level: 2 },
  { id: "openapi2kotlin-server", label: "Server", level: 2 },
];
