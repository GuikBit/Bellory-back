import { useState, useEffect } from 'react';
import { PanelBottom, Plus, Trash2 } from 'lucide-react';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { FooterSectionSettings } from '../../../../../types/section-settings.types';

interface FooterSectionEditorProps {
  section: LandingPageSection;
  onUpdateSettings: (settings: Record<string, unknown>) => void;
}

export function FooterSectionEditor({ section, onUpdateSettings }: FooterSectionEditorProps) {
  const [settings, setSettings] = useState<FooterSectionSettings>(
    (section.settings as FooterSectionSettings) ?? {}
  );

  useEffect(() => {
    setSettings((section.settings as FooterSectionSettings) ?? {});
  }, [section.sectionId]);

  const update = (partial: Partial<FooterSectionSettings>) => {
    const next = { ...settings, ...partial };
    setSettings(next);
    onUpdateSettings(next as Record<string, unknown>);
  };

  return (
    <div className="p-4 space-y-5">
      <div>
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <PanelBottom size={16} className="text-primary" />
          Rodapé
        </h3>
        <p className="text-xs text-muted-foreground mt-1">Configure o rodapé</p>
      </div>

      <div className="space-y-1.5">
        <label className="text-xs font-medium text-muted-foreground">Texto de Copyright</label>
        <input
          type="text"
          value={settings.copyrightText ?? ''}
          onChange={(e) => update({ copyrightText: e.target.value || undefined })}
          placeholder="© 2024 Sua Empresa. Todos os direitos reservados."
          className="w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      {/* Link Sections */}
      <div className="space-y-2">
        <label className="text-xs font-medium text-muted-foreground">Seções de Links</label>
        {(settings.linkSections ?? []).map((section, i) => (
          <div key={i} className="border rounded-lg p-3 space-y-2">
            <div className="flex items-center justify-between">
              <input
                type="text"
                value={section.title}
                onChange={(e) => {
                  const sections = [...(settings.linkSections ?? [])];
                  sections[i] = { ...sections[i], title: e.target.value };
                  update({ linkSections: sections });
                }}
                placeholder="Título da seção"
                className="flex-1 px-2 py-1 border rounded text-xs focus:outline-none focus:ring-1 focus:ring-primary/50"
              />
              <button
                onClick={() => {
                  const sections = (settings.linkSections ?? []).filter((_, idx) => idx !== i);
                  update({ linkSections: sections });
                }}
                className="p-1 text-red-500 hover:bg-red-50 rounded ml-2"
              >
                <Trash2 size={12} />
              </button>
            </div>
            {section.links.map((link, j) => (
              <div key={j} className="flex gap-1.5 ml-2">
                <input
                  type="text"
                  value={link.label}
                  onChange={(e) => {
                    const sections = [...(settings.linkSections ?? [])];
                    const links = [...sections[i].links];
                    links[j] = { ...links[j], label: e.target.value };
                    sections[i] = { ...sections[i], links };
                    update({ linkSections: sections });
                  }}
                  placeholder="Label"
                  className="flex-1 px-2 py-1 border rounded text-xs"
                />
                <input
                  type="text"
                  value={link.href}
                  onChange={(e) => {
                    const sections = [...(settings.linkSections ?? [])];
                    const links = [...sections[i].links];
                    links[j] = { ...links[j], href: e.target.value };
                    sections[i] = { ...sections[i], links };
                    update({ linkSections: sections });
                  }}
                  placeholder="Link"
                  className="flex-1 px-2 py-1 border rounded text-xs"
                />
                <button
                  onClick={() => {
                    const sections = [...(settings.linkSections ?? [])];
                    const links = sections[i].links.filter((_, idx) => idx !== j);
                    sections[i] = { ...sections[i], links };
                    update({ linkSections: sections });
                  }}
                  className="p-1 text-red-400 hover:bg-red-50 rounded"
                >
                  <Trash2 size={10} />
                </button>
              </div>
            ))}
            <button
              onClick={() => {
                const sections = [...(settings.linkSections ?? [])];
                const links = [...sections[i].links, { label: '', href: '#' }];
                sections[i] = { ...sections[i], links };
                update({ linkSections: sections });
              }}
              className="flex items-center gap-1 text-xs text-primary hover:underline ml-2"
            >
              <Plus size={10} />
              Adicionar Link
            </button>
          </div>
        ))}
        <button
          onClick={() => {
            const sections = [...(settings.linkSections ?? []), { title: 'Nova Seção', links: [] }];
            update({ linkSections: sections });
          }}
          className="flex items-center gap-1.5 text-xs text-primary hover:underline"
        >
          <Plus size={12} />
          Adicionar Seção de Links
        </button>
      </div>

      {/* Toggles */}
      <div className="space-y-2">
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            checked={settings.showHours ?? true}
            onChange={(e) => update({ showHours: e.target.checked })}
            className="rounded border-gray-300 text-primary focus:ring-primary"
          />
          Exibir horários
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
        <label className="flex items-center gap-2 text-sm cursor-pointer">
          <input
            type="checkbox"
            checked={settings.showNewsletter ?? false}
            onChange={(e) => update({ showNewsletter: e.target.checked })}
            className="rounded border-gray-300 text-primary focus:ring-primary"
          />
          Exibir newsletter
        </label>
      </div>
    </div>
  );
}
