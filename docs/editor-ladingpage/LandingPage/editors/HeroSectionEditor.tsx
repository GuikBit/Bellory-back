import { useState, useEffect } from 'react';
import { Image, Type, MousePointer, BarChart3, Plus, Trash2 } from 'lucide-react';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { HeroSectionSettings } from '../../../../../types/section-settings.types';
import type { ActionButton } from '../../../../../types/site.types';

interface HeroSectionEditorProps {
  section: LandingPageSection;
  onUpdateSettings: (settings: Record<string, unknown>) => void;
}

export function HeroSectionEditor({ section, onUpdateSettings }: HeroSectionEditorProps) {
  const [settings, setSettings] = useState<HeroSectionSettings>(
    (section.settings as HeroSectionSettings) ?? {}
  );

  useEffect(() => {
    setSettings((section.settings as HeroSectionSettings) ?? {});
  }, [section.sectionId]);

  const update = (partial: Partial<HeroSectionSettings>) => {
    const next = { ...settings, ...partial };
    setSettings(next);
    onUpdateSettings(next as Record<string, unknown>);
  };

  const updateButton = (index: number, field: keyof ActionButton, value: string) => {
    const buttons = [...(settings.buttons ?? [])];
    buttons[index] = { ...buttons[index], [field]: value };
    update({ buttons });
  };

  const addButton = () => {
    const buttons = [...(settings.buttons ?? []), { label: 'Novo Botão', href: '#', type: 'primary' as const }];
    update({ buttons });
  };

  const removeButton = (index: number) => {
    const buttons = (settings.buttons ?? []).filter((_, i) => i !== index);
    update({ buttons });
  };

  return (
    <div className="p-4 space-y-5">
      <div>
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <Type size={16} className="text-primary" />
          Hero / Apresentação
        </h3>
        <p className="text-xs text-muted-foreground mt-1">
          Personalize a seção principal da sua página
        </p>
      </div>

      {/* Título */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Título</label>
        <input
          type="text"
          value={settings.titulo ?? ''}
          onChange={(e) => update({ titulo: e.target.value || undefined })}
          placeholder="Título principal..."
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      {/* Subtítulo */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Subtítulo</label>
        <textarea
          value={settings.subtitulo ?? ''}
          onChange={(e) => update({ subtitulo: e.target.value || undefined })}
          placeholder="Subtítulo ou descrição curta..."
          rows={3}
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none"
        />
      </div>

      {/* Imagem de Fundo */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <Image size={12} />
          Imagem de Fundo
        </label>
        <input
          type="text"
          value={settings.backgroundImage ?? ''}
          onChange={(e) => update({ backgroundImage: e.target.value || undefined })}
          placeholder="URL da imagem..."
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      {/* Overlay */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">
          Overlay ({Math.round((settings.backgroundOverlay ?? 0.5) * 100)}%)
        </label>
        <input
          type="range"
          min={0}
          max={1}
          step={0.05}
          value={settings.backgroundOverlay ?? 0.5}
          onChange={(e) => update({ backgroundOverlay: parseFloat(e.target.value) })}
          className="w-full accent-primary"
        />
      </div>

      {/* Botões */}
      <div className="space-y-2">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <MousePointer size={12} />
          Botões
        </label>
        {(settings.buttons ?? []).map((btn, i) => (
          <div key={i} className="flex gap-2 items-start">
            <div className="flex-1 space-y-1">
              <input
                type="text"
                value={btn.label}
                onChange={(e) => updateButton(i, 'label', e.target.value)}
                placeholder="Texto do botão"
                className="w-full px-2 py-1.5 border rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary/50"
              />
              <input
                type="text"
                value={btn.href}
                onChange={(e) => updateButton(i, 'href', e.target.value)}
                placeholder="Link (ex: #servicos)"
                className="w-full px-2 py-1.5 border rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary/50"
              />
            </div>
            <button
              onClick={() => removeButton(i)}
              className="p-1.5 text-red-500 hover:bg-red-50 rounded mt-1"
            >
              <Trash2 size={14} />
            </button>
          </div>
        ))}
        <button
          onClick={addButton}
          className="flex items-center gap-1.5 text-xs text-primary hover:underline"
        >
          <Plus size={12} />
          Adicionar Botão
        </button>
      </div>

      {/* Stats */}
      <div className="space-y-2">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <BarChart3 size={12} />
          Estatísticas
        </label>
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            checked={settings.showStats ?? true}
            onChange={(e) => update({ showStats: e.target.checked })}
            className="rounded border-gray-300 text-primary focus:ring-primary"
          />
          Exibir estatísticas
        </label>
      </div>
    </div>
  );
}
