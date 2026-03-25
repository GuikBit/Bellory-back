import { useState, useEffect } from 'react';
import { Info, Image, Video, Plus, Trash2 } from 'lucide-react';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { AboutSectionSettings } from '../../../../../types/section-settings.types';

interface AboutSectionEditorProps {
  section: LandingPageSection;
  onUpdateSettings: (settings: Record<string, unknown>) => void;
}

export function AboutSectionEditor({ section, onUpdateSettings }: AboutSectionEditorProps) {
  const [settings, setSettings] = useState<AboutSectionSettings>(
    (section.settings as AboutSectionSettings) ?? {}
  );

  useEffect(() => {
    setSettings((section.settings as AboutSectionSettings) ?? {});
  }, [section.sectionId]);

  const update = (partial: Partial<AboutSectionSettings>) => {
    const next = { ...settings, ...partial };
    setSettings(next);
    onUpdateSettings(next as Record<string, unknown>);
  };

  return (
    <div className="p-4 space-y-5">
      <div>
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <Info size={16} className="text-primary" />
          Sobre
        </h3>
        <p className="text-xs text-muted-foreground mt-1">Configure a seção Sobre</p>
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
        <label className="text-xs font-medium text-muted-foreground">Descrição</label>
        <textarea
          value={settings.descricao ?? ''}
          onChange={(e) => update({ descricao: e.target.value || undefined })}
          placeholder="Descrição sobre a empresa..."
          rows={4}
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none"
        />
      </div>

      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <Image size={12} />
          Imagem
        </label>
        <input
          type="text"
          value={settings.imagem ?? ''}
          onChange={(e) => update({ imagem: e.target.value || undefined })}
          placeholder="URL da imagem..."
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
          <Video size={12} />
          URL do Vídeo
        </label>
        <input
          type="text"
          value={settings.videoUrl ?? ''}
          onChange={(e) => update({ videoUrl: e.target.value || undefined })}
          placeholder="URL do vídeo (YouTube, Vimeo...)"
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      {/* Highlights */}
      <div className="space-y-2">
        <label className="text-xs font-medium text-muted-foreground">Destaques</label>
        {(settings.highlights ?? []).map((h, i) => (
          <div key={i} className="flex gap-2">
            <input
              type="text"
              value={h}
              onChange={(e) => {
                const highlights = [...(settings.highlights ?? [])];
                highlights[i] = e.target.value;
                update({ highlights });
              }}
              className="flex-1 px-2 py-1.5 border rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary/50"
            />
            <button
              onClick={() => {
                const highlights = (settings.highlights ?? []).filter((_, idx) => idx !== i);
                update({ highlights });
              }}
              className="p-1.5 text-red-500 hover:bg-red-50 rounded"
            >
              <Trash2 size={12} />
            </button>
          </div>
        ))}
        <button
          onClick={() => update({ highlights: [...(settings.highlights ?? []), ''] })}
          className="flex items-center gap-1.5 text-xs text-primary hover:underline"
        >
          <Plus size={12} />
          Adicionar Destaque
        </button>
      </div>

      {/* Missão */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Missão</label>
        <textarea
          value={settings.missao ?? ''}
          onChange={(e) => update({ missao: e.target.value || undefined })}
          placeholder="Nossa missão..."
          rows={2}
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none"
        />
      </div>

      {/* Visão */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Visão</label>
        <textarea
          value={settings.visao ?? ''}
          onChange={(e) => update({ visao: e.target.value || undefined })}
          placeholder="Nossa visão..."
          rows={2}
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none"
        />
      </div>

      {/* Valores */}
      <div className="space-y-2">
        <label className="text-xs font-medium text-muted-foreground">Valores</label>
        {(settings.valores ?? []).map((v, i) => (
          <div key={i} className="flex gap-2">
            <input
              type="text"
              value={v}
              onChange={(e) => {
                const valores = [...(settings.valores ?? [])];
                valores[i] = e.target.value;
                update({ valores });
              }}
              className="flex-1 px-2 py-1.5 border rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary/50"
            />
            <button
              onClick={() => {
                const valores = (settings.valores ?? []).filter((_, idx) => idx !== i);
                update({ valores });
              }}
              className="p-1.5 text-red-500 hover:bg-red-50 rounded"
            >
              <Trash2 size={12} />
            </button>
          </div>
        ))}
        <button
          onClick={() => update({ valores: [...(settings.valores ?? []), ''] })}
          className="flex items-center gap-1.5 text-xs text-primary hover:underline"
        >
          <Plus size={12} />
          Adicionar Valor
        </button>
      </div>

      <label className="flex items-center gap-2 text-sm cursor-pointer">
        <input
          type="checkbox"
          checked={settings.showOrganizationInfo ?? true}
          onChange={(e) => update({ showOrganizationInfo: e.target.checked })}
          className="rounded border-gray-300 text-primary focus:ring-primary"
        />
        Exibir informações da organização
      </label>
    </div>
  );
}
