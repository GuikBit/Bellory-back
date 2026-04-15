import { Puzzle } from 'lucide-react';
import { StyleEditor } from '../fragments/StyleEditor';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { EditorStore } from '../../../../../types/landing-page.types';

interface GenericSectionEditorProps {
  section: LandingPageSection;
  editor: EditorStore;
}

export function GenericSectionEditor({ section, editor }: GenericSectionEditorProps) {
  return (
    <div className="p-4 space-y-4">
      <div>
        <h3 className="font-semibold text-sm flex items-center gap-2">
          <Puzzle size={16} className="text-primary" />
          Seção Personalizada
        </h3>
        <p className="text-xs text-muted-foreground mt-1">
          {section.nome || section.tipo}
        </p>
      </div>

      <div>
        <label className="text-xs font-medium text-muted-foreground">Nome</label>
        <input
          type="text"
          value={section.nome || ''}
          onChange={(e) =>
            editor.updateSection(section.sectionId, { nome: e.target.value })
          }
          className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>

      <StyleEditor
        styles={section.styles}
        onChange={(styles) =>
          editor.updateSection(section.sectionId, { styles })
        }
      />
    </div>
  );
}
