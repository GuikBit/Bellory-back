import { useState, useEffect } from 'react';
import { Scissors, LayoutGrid } from 'lucide-react';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { ServicesSectionSettings } from '../../../../../types/section-settings.types';
import { useEditorSiteData } from '../../../../../hooks/useEditorSiteData';

interface ServicesSectionEditorProps {
  section: LandingPageSection;
  onUpdateSettings: (settings: Record<string, unknown>) => void;
}

export function ServicesSectionEditor({ section, onUpdateSettings }: ServicesSectionEditorProps) {
  const [settings, setSettings] = useState<ServicesSectionSettings>(
    (section.settings as ServicesSectionSettings) ?? {}
  );
  const { homeData } = useEditorSiteData();
  const categorias = homeData?.services?.categorias ?? [];

  useEffect(() => {
    setSettings((section.settings as ServicesSectionSettings) ?? {});
  }, [section.sectionId]);

  const update = (partial: Partial<ServicesSectionSettings>) => {
    const next = { ...settings, ...partial };
    setSettings(next);
    onUpdateSettings(next as Record<string, unknown>);
  };

  const toggleCategory = (catId: number) => {
    const current = settings.categoryFilter ?? [];
    const next = current.includes(catId)
      ? current.filter(id => id !== catId)
      : [...current, catId];
    update({ categoryFilter: next.length > 0 ? next : undefined });
  };

  return (
    <div className="p-4 space-y-5">
      <div>
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <Scissors size={16} className="text-primary" />
          Serviços
        </h3>
        <p className="text-xs text-muted-foreground mt-1">Configure a seção de serviços</p>
      </div>

      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Título</label>
        <input
          type="text"
          value={settings.titulo ?? ''}
          onChange={(e) => update({ titulo: e.target.value || undefined })}
          placeholder="Título da seção..."
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Subtítulo</label>
        <input
          type="text"
          value={settings.subtitulo ?? ''}
          onChange={(e) => update({ subtitulo: e.target.value || undefined })}
          placeholder="Subtítulo..."
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Máximo de itens</label>
        <input
          type="number"
          min={1}
          max={50}
          value={settings.maxItems ?? ''}
          onChange={(e) => update({ maxItems: e.target.value ? parseInt(e.target.value) : undefined })}
          placeholder="Todos"
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      {/* Filtro de Categorias */}
      {categorias.length > 0 && (
        <div className="space-y-2">
          <label className="text-xs font-medium text-muted-foreground">Filtrar por Categorias</label>
          <div className="space-y-1.5 max-h-40 overflow-y-auto">
            {categorias.map((cat) => (
              <label key={cat.id} className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={(settings.categoryFilter ?? []).includes(cat.id)}
                  onChange={() => toggleCategory(cat.id)}
                  className="rounded border-gray-300 text-primary focus:ring-primary"
                />
                {cat.label} ({cat.quantidadeServicos})
              </label>
            ))}
          </div>
        </div>
      )}

      {/* Layout */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <LayoutGrid size={12} />
          Layout
        </label>
        <select
          value={settings.layout ?? 'grid'}
          onChange={(e) => update({ layout: e.target.value as ServicesSectionSettings['layout'] })}
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        >
          <option value="grid">Grade</option>
          <option value="list">Lista</option>
          <option value="carousel">Carrossel</option>
        </select>
      </div>

      {/* Toggles */}
      <div className="space-y-2">
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            checked={settings.showPrices ?? true}
            onChange={(e) => update({ showPrices: e.target.checked })}
            className="rounded border-gray-300 text-primary focus:ring-primary"
          />
          Exibir preços
        </label>
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            checked={settings.showDuration ?? true}
            onChange={(e) => update({ showDuration: e.target.checked })}
            className="rounded border-gray-300 text-primary focus:ring-primary"
          />
          Exibir duração
        </label>
      </div>
    </div>
  );
}
