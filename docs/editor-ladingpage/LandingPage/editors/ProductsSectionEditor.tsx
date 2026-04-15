import { useState, useEffect } from 'react';
import { ShoppingBag } from 'lucide-react';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { ProductsSectionSettings } from '../../../../../types/section-settings.types';

interface ProductsSectionEditorProps {
  section: LandingPageSection;
  onUpdateSettings: (settings: Record<string, unknown>) => void;
}

export function ProductsSectionEditor({ section, onUpdateSettings }: ProductsSectionEditorProps) {
  const [settings, setSettings] = useState<ProductsSectionSettings>(
    (section.settings as ProductsSectionSettings) ?? {}
  );

  useEffect(() => {
    setSettings((section.settings as ProductsSectionSettings) ?? {});
  }, [section.sectionId]);

  const update = (partial: Partial<ProductsSectionSettings>) => {
    const next = { ...settings, ...partial };
    setSettings(next);
    onUpdateSettings(next as Record<string, unknown>);
  };

  return (
    <div className="p-4 space-y-5">
      <div>
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <ShoppingBag size={16} className="text-primary" />
          Produtos
        </h3>
        <p className="text-xs text-muted-foreground mt-1">Configure a seção de produtos</p>
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

      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Velocidade do Carrossel (ms)</label>
        <input
          type="number"
          min={1000}
          max={10000}
          step={500}
          value={settings.autoPlaySpeed ?? ''}
          onChange={(e) => update({ autoPlaySpeed: e.target.value ? parseInt(e.target.value) : undefined })}
          placeholder="5000"
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      <label className="flex items-center gap-2 text-sm cursor-pointer">
        <input
          type="checkbox"
          checked={settings.showPrices ?? true}
          onChange={(e) => update({ showPrices: e.target.checked })}
          className="rounded border-gray-300 text-primary focus:ring-primary"
        />
        Exibir preços
      </label>
    </div>
  );
}
