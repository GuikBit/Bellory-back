import { useState, useEffect } from 'react';
import { Menu, Plus, Trash2 } from 'lucide-react';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { HeaderSectionSettings } from '../../../../../types/section-settings.types';

interface HeaderSectionEditorProps {
  section: LandingPageSection;
  onUpdateSettings: (settings: Record<string, unknown>) => void;
}

export function HeaderSectionEditor({ section, onUpdateSettings }: HeaderSectionEditorProps) {
  const [settings, setSettings] = useState<HeaderSectionSettings>(
    (section.settings as HeaderSectionSettings) ?? {}
  );

  useEffect(() => {
    setSettings((section.settings as HeaderSectionSettings) ?? {});
  }, [section.sectionId]);

  const update = (partial: Partial<HeaderSectionSettings>) => {
    const next = { ...settings, ...partial };
    setSettings(next);
    onUpdateSettings(next as Record<string, unknown>);
  };

  return (
    <div className="p-4 space-y-5">
      <div>
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <Menu size={16} className="text-primary" />
          Header / Menu
        </h3>
        <p className="text-xs text-muted-foreground mt-1">Configure o cabeçalho</p>
      </div>

      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">URL do Logo</label>
        <input
          type="text"
          value={settings.logoUrl ?? ''}
          onChange={(e) => update({ logoUrl: e.target.value || undefined })}
          placeholder="URL da imagem do logo..."
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      {/* Menu Items */}
      <div className="space-y-2">
        <label className="text-xs font-medium text-muted-foreground">Itens do Menu</label>
        {(settings.menuItems ?? []).map((item, i) => (
          <div key={i} className="flex gap-2 items-start">
            <div className="flex-1 space-y-1">
              <input
                type="text"
                value={item.label}
                onChange={(e) => {
                  const items = [...(settings.menuItems ?? [])];
                  items[i] = { ...items[i], label: e.target.value };
                  update({ menuItems: items });
                }}
                placeholder="Label"
                className="w-full px-2 py-1.5 border rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary/50"
              />
              <input
                type="text"
                value={item.href}
                onChange={(e) => {
                  const items = [...(settings.menuItems ?? [])];
                  items[i] = { ...items[i], href: e.target.value };
                  update({ menuItems: items });
                }}
                placeholder="Link (ex: #servicos)"
                className="w-full px-2 py-1.5 border rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary/50"
              />
            </div>
            <button
              onClick={() => {
                const items = (settings.menuItems ?? []).filter((_, idx) => idx !== i);
                update({ menuItems: items });
              }}
              className="p-1.5 text-red-500 hover:bg-red-50 rounded mt-1"
            >
              <Trash2 size={14} />
            </button>
          </div>
        ))}
        <button
          onClick={() => {
            const items = [...(settings.menuItems ?? []), { label: '', href: '#', order: (settings.menuItems?.length ?? 0) }];
            update({ menuItems: items });
          }}
          className="flex items-center gap-1.5 text-xs text-primary hover:underline"
        >
          <Plus size={12} />
          Adicionar Item
        </button>
      </div>

      {/* Toggles */}
      <div className="space-y-2">
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            checked={settings.sticky ?? true}
            onChange={(e) => update({ sticky: e.target.checked })}
            className="rounded border-gray-300 text-primary focus:ring-primary"
          />
          Menu fixo no topo
        </label>
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            checked={settings.showPhone ?? true}
            onChange={(e) => update({ showPhone: e.target.checked })}
            className="rounded border-gray-300 text-primary focus:ring-primary"
          />
          Exibir telefone
        </label>
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            checked={settings.showSocial ?? true}
            onChange={(e) => update({ showSocial: e.target.checked })}
            className="rounded border-gray-300 text-primary focus:ring-primary"
          />
          Exibir redes sociais
        </label>
      </div>
    </div>
  );
}
