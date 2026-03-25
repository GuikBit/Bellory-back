import { useState, useEffect } from 'react';
import { Users } from 'lucide-react';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { TeamSectionSettings } from '../../../../../types/section-settings.types';
import { useEditorSiteData } from '../../../../../hooks/useEditorSiteData';

interface TeamSectionEditorProps {
  section: LandingPageSection;
  onUpdateSettings: (settings: Record<string, unknown>) => void;
}

export function TeamSectionEditor({ section, onUpdateSettings }: TeamSectionEditorProps) {
  const [settings, setSettings] = useState<TeamSectionSettings>(
    (section.settings as TeamSectionSettings) ?? {}
  );
  const { homeData } = useEditorSiteData();
  const membros = homeData?.team?.membros ?? [];

  useEffect(() => {
    setSettings((section.settings as TeamSectionSettings) ?? {});
  }, [section.sectionId]);

  const update = (partial: Partial<TeamSectionSettings>) => {
    const next = { ...settings, ...partial };
    setSettings(next);
    onUpdateSettings(next as Record<string, unknown>);
  };

  const toggleMember = (memberId: number) => {
    const current = settings.memberIds ?? [];
    const next = current.includes(memberId)
      ? current.filter(id => id !== memberId)
      : [...current, memberId];
    update({ memberIds: next.length > 0 ? next : undefined });
  };

  return (
    <div className="p-4 space-y-5">
      <div>
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <Users size={16} className="text-primary" />
          Equipe
        </h3>
        <p className="text-xs text-muted-foreground mt-1">Configure a seção da equipe</p>
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

      {/* Filtro de Membros */}
      {membros.length > 0 && (
        <div className="space-y-2">
          <label className="text-xs font-medium text-muted-foreground">Filtrar Membros</label>
          <p className="text-xs text-muted-foreground">
            Deixe todos desmarcados para exibir todos
          </p>
          <div className="space-y-1.5 max-h-48 overflow-y-auto">
            {membros.map((m) => (
              <label key={m.id} className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={(settings.memberIds ?? []).includes(m.id)}
                  onChange={() => toggleMember(m.id)}
                  className="rounded border-gray-300 text-primary focus:ring-primary"
                />
                <span>{m.apelido || m.nome}</span>
                <span className="text-xs text-muted-foreground">({m.cargo})</span>
              </label>
            ))}
          </div>
        </div>
      )}

      <label className="flex items-center gap-2 text-sm cursor-pointer">
        <input
          type="checkbox"
          checked={settings.showSchedule ?? false}
          onChange={(e) => update({ showSchedule: e.target.checked })}
          className="rounded border-gray-300 text-primary focus:ring-primary"
        />
        Exibir horários
      </label>
    </div>
  );
}
