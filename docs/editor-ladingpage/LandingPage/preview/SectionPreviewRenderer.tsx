import { lazy, Suspense } from 'react';
import { cn } from '../../../../../lib/utils';
import { Eye, EyeOff, GripVertical, Copy, Trash2, Settings } from 'lucide-react';
import type { LandingPageSection, DeviceType } from '../../../../../types/landing-page.types';
import { SectionRenderer } from '../fragments/SectionRenderer';

// Lazy load dos componentes externos para melhor performance
const SiteHeader = lazy(() => import('../../../../Externo/Header/SiteHeader'));
const SiteHero = lazy(() => import('../../../../Externo/Hero/SiteHero'));
const SiteAbout = lazy(() => import('../../../../Externo/About/SiteAbout'));
const SiteServicosDestaque = lazy(() => import('../../../../Externo/ServicosDestaque/SiteServicosDestaque'));
const SiteProdutosDestaque = lazy(() => import('../../../../Externo/ProdutosDestaque/SiteProdutosDestaque'));
const SiteEquipeDestaque = lazy(() => import('../../../../Externo/EquipeDestaque/SiteEquipeDestaque'));
const SiteAgendamento = lazy(() => import('../../../../Externo/Agendamento/SiteAgendamento'));
const SiteFooter = lazy(() => import('../../../../Externo/Footer/SiteFooter'));

// Tipos de seção que renderizam componentes reais
const REAL_COMPONENT_TYPES = new Set([
  'HEADER', 'HERO', 'ABOUT', 'SERVICES', 'PRODUCTS', 'TEAM', 'BOOKING', 'FOOTER',
]);

interface SectionPreviewRendererProps {
  section: LandingPageSection;
  device: DeviceType;
  variables?: Record<string, string>;
  isSelected?: boolean;
  isEditing?: boolean;
  selectedElementId?: string | null;
  onClick?: () => void;
  onElementClick?: (elementId: string) => void;
  onToggleVisibility?: () => void;
  onDuplicate?: () => void;
  onDelete?: () => void;
  onSettings?: () => void;
  onAddElement?: () => void;
  showControls?: boolean;
  className?: string;
}

function SectionLoadingFallback() {
  return (
    <div className="py-12 flex items-center justify-center">
      <div className="w-8 h-8 rounded-full border-2 border-primary/20 border-t-primary animate-spin" />
    </div>
  );
}

function RealComponentByType({ tipo }: { tipo: string }) {
  switch (tipo) {
    case 'HEADER':
      return <SiteHeader />;
    case 'HERO':
      return <SiteHero />;
    case 'ABOUT':
      return <SiteAbout />;
    case 'SERVICES':
      return <SiteServicosDestaque />;
    case 'PRODUCTS':
      return <SiteProdutosDestaque />;
    case 'TEAM':
      return <SiteEquipeDestaque />;
    case 'BOOKING':
      return <SiteAgendamento />;
    case 'FOOTER':
      return <SiteFooter />;
    default:
      return null;
  }
}

export function SectionPreviewRenderer({
  section,
  device,
  variables = {},
  isSelected,
  isEditing,
  selectedElementId,
  onClick,
  onElementClick,
  onToggleVisibility,
  onDuplicate,
  onDelete,
  onSettings,
  onAddElement,
  showControls = true,
  className,
}: SectionPreviewRendererProps) {
  // Se a seção está oculta e não estamos editando, não renderiza
  if (!section.visible && !isEditing) {
    return null;
  }

  // Para seções CUSTOM ou tipos não mapeados, usa o SectionRenderer genérico
  if (!REAL_COMPONENT_TYPES.has(section.tipo)) {
    return (
      <SectionRenderer
        section={section}
        device={device}
        variables={variables}
        isSelected={isSelected}
        isEditing={isEditing}
        selectedElementId={selectedElementId}
        onClick={onClick}
        onElementClick={onElementClick}
        onToggleVisibility={onToggleVisibility}
        onDuplicate={onDuplicate}
        onDelete={onDelete}
        onSettings={onSettings}
        onAddElement={onAddElement}
        showControls={showControls}
        className={className}
      />
    );
  }

  // Renderiza componente real envolvido no SectionWrapper
  return (
    <div
      className={cn(
        'relative group transition-all duration-200 cursor-pointer',
        isSelected && 'ring-2 ring-primary ring-inset',
        !section.visible && 'opacity-40',
        className
      )}
      onClick={onClick}
      data-section-id={section.sectionId}
    >
      {/* Controls overlay */}
      {showControls && isEditing && (
        <div
          className={cn(
            'absolute top-2 left-2 right-2 flex items-center justify-between z-20 opacity-0 group-hover:opacity-100 transition-opacity',
            isSelected && 'opacity-100'
          )}
        >
          <div className="flex items-center gap-1 bg-white dark:bg-neutral-800 rounded-lg shadow-md p-1">
            <button className="p-1.5 hover:bg-secondary rounded cursor-grab" title="Arrastar">
              <GripVertical size={14} className="text-muted-foreground" />
            </button>
            <span className="px-2 text-xs font-medium text-muted-foreground">
              {section.nome || section.tipo}
            </span>
          </div>

          <div className="flex items-center gap-1 bg-white dark:bg-neutral-800 rounded-lg shadow-md p-1">
            <button
              onClick={(e) => { e.stopPropagation(); onToggleVisibility?.(); }}
              className="p-1.5 hover:bg-secondary rounded"
              title={section.visible ? 'Ocultar' : 'Mostrar'}
            >
              {section.visible ? (
                <Eye size={14} className="text-muted-foreground" />
              ) : (
                <EyeOff size={14} className="text-muted-foreground" />
              )}
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); onDuplicate?.(); }}
              className="p-1.5 hover:bg-secondary rounded"
              title="Duplicar"
            >
              <Copy size={14} className="text-muted-foreground" />
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); onSettings?.(); }}
              className="p-1.5 hover:bg-secondary rounded"
              title="Configurações"
            >
              <Settings size={14} className="text-muted-foreground" />
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); onDelete?.(); }}
              className="p-1.5 hover:bg-secondary rounded text-red-500"
              title="Excluir"
            >
              <Trash2 size={14} />
            </button>
          </div>
        </div>
      )}

      {/* Componente real */}
      <div className="relative z-0 pointer-events-none">
        <Suspense fallback={<SectionLoadingFallback />}>
          <RealComponentByType tipo={section.tipo} />
        </Suspense>
      </div>
    </div>
  );
}
