import { useCallback } from 'react';
import type { LandingPageSection } from '../../../../../types/landing-page.types';
import type { EditorStore } from '../../../../../types/landing-page.types';
import { HeroSectionEditor } from './HeroSectionEditor';
import { AboutSectionEditor } from './AboutSectionEditor';
import { ServicesSectionEditor } from './ServicesSectionEditor';
import { ProductsSectionEditor } from './ProductsSectionEditor';
import { TeamSectionEditor } from './TeamSectionEditor';
import { HeaderSectionEditor } from './HeaderSectionEditor';
import { FooterSectionEditor } from './FooterSectionEditor';
import { BookingSectionEditor } from './BookingSectionEditor';
import { GenericSectionEditor } from './GenericSectionEditor';

interface SectionEditorRouterProps {
  section: LandingPageSection;
  editor: EditorStore;
}

export function SectionEditorRouter({ section, editor }: SectionEditorRouterProps) {
  const handleUpdateSettings = useCallback((settings: Record<string, unknown>) => {
    editor.updateSection(section.sectionId, {
      settings: { ...(section.settings ?? {}), ...settings },
    });
  }, [editor, section.sectionId, section.settings]);

  switch (section.tipo) {
    case 'HERO':
      return <HeroSectionEditor section={section} onUpdateSettings={handleUpdateSettings} />;
    case 'ABOUT':
      return <AboutSectionEditor section={section} onUpdateSettings={handleUpdateSettings} />;
    case 'SERVICES':
      return <ServicesSectionEditor section={section} onUpdateSettings={handleUpdateSettings} />;
    case 'PRODUCTS':
      return <ProductsSectionEditor section={section} onUpdateSettings={handleUpdateSettings} />;
    case 'TEAM':
      return <TeamSectionEditor section={section} onUpdateSettings={handleUpdateSettings} />;
    case 'HEADER':
      return <HeaderSectionEditor section={section} onUpdateSettings={handleUpdateSettings} />;
    case 'FOOTER':
      return <FooterSectionEditor section={section} onUpdateSettings={handleUpdateSettings} />;
    case 'BOOKING':
      return <BookingSectionEditor section={section} onUpdateSettings={handleUpdateSettings} />;
    default:
      return <GenericSectionEditor section={section} editor={editor} />;
  }
}
