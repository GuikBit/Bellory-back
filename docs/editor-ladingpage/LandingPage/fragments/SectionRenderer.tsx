import { cn } from '../../../../../lib/utils';
import type { LandingPageSection, DeviceType } from '../../../../../types/landing-page.types';
import { useResponsiveStyles } from '../../../../../hooks/useResponsiveStyles';
import { ElementRenderer } from '../elements/ElementRenderer';
import { Eye, EyeOff, GripVertical, Copy, Trash2, Settings, Plus } from 'lucide-react';

interface SectionRendererProps {
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

export function SectionRenderer({
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
}: SectionRendererProps) {
  const styles = useResponsiveStyles(section.styles, device);

  // Estilos de background
  const backgroundStyles: React.CSSProperties = {};
  if (section.background) {
    switch (section.background.type) {
      case 'color':
        backgroundStyles.backgroundColor = section.background.color;
        break;
      case 'gradient':
        backgroundStyles.background = section.background.gradient;
        break;
      case 'image':
        backgroundStyles.backgroundImage = `url(${section.background.imageUrl})`;
        backgroundStyles.backgroundPosition = section.background.position || 'center';
        backgroundStyles.backgroundSize = section.background.size || 'cover';
        backgroundStyles.backgroundRepeat = section.background.repeat || 'no-repeat';
        break;
    }
  }

  // Classes de layout
  const layoutClasses = cn(
    'w-full',
    section.content.layout === 'contained' && 'max-w-7xl mx-auto px-4',
    section.content.layout === 'narrow' && 'max-w-4xl mx-auto px-4',
    section.content.alignment === 'center' && 'text-center',
    section.content.alignment === 'right' && 'text-right'
  );

  // Se a seção está oculta e não estamos editando
  if (!section.visible && !isEditing) {
    return null;
  }

  return (
    <section
      className={cn(
        'relative group transition-all duration-200',
        isSelected && 'ring-2 ring-primary ring-inset',
        !section.visible && 'opacity-50',
        className
      )}
      style={{
        ...backgroundStyles,
        ...styles,
      }}
      onClick={onClick}
      data-section-id={section.sectionId}
    >
      {/* Overlay para background com imagem */}
      {section.background?.type === 'image' && section.background.overlay && (
        <div
          className="absolute inset-0 bg-black pointer-events-none"
          style={{ opacity: section.background.overlay }}
        />
      )}

      {/* Controls */}
      {showControls && isEditing && (
        <div
          className={cn(
            'absolute top-2 left-2 right-2 flex items-center justify-between z-10 opacity-0 group-hover:opacity-100 transition-opacity',
            isSelected && 'opacity-100'
          )}
        >
          {/* Left controls */}
          <div className="flex items-center gap-1 bg-white dark:bg-neutral-800 rounded-lg shadow-md p-1">
            <button
              className="p-1.5 hover:bg-secondary rounded cursor-grab"
              title="Arrastar"
            >
              <GripVertical size={14} className="text-muted-foreground" />
            </button>
            <span className="px-2 text-xs font-medium text-muted-foreground">
              {section.nome || section.tipo}
            </span>
          </div>

          {/* Right controls */}
          <div className="flex items-center gap-1 bg-white dark:bg-neutral-800 rounded-lg shadow-md p-1">
            <button
              onClick={(e) => {
                e.stopPropagation();
                onToggleVisibility?.();
              }}
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
              onClick={(e) => {
                e.stopPropagation();
                onDuplicate?.();
              }}
              className="p-1.5 hover:bg-secondary rounded"
              title="Duplicar"
            >
              <Copy size={14} className="text-muted-foreground" />
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onSettings?.();
              }}
              className="p-1.5 hover:bg-secondary rounded"
              title="Configurações"
            >
              <Settings size={14} className="text-muted-foreground" />
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onDelete?.();
              }}
              className="p-1.5 hover:bg-secondary rounded text-red-500"
              title="Excluir"
            >
              <Trash2 size={14} />
            </button>
          </div>
        </div>
      )}

      {/* Content */}
      <div className={cn('relative z-0', layoutClasses)}>
        {section.content.elements.length === 0 ? (
          <div
            className="py-16 text-center border-2 border-dashed rounded-xl text-muted-foreground cursor-pointer hover:border-primary hover:bg-primary/5 transition-colors"
            onClick={(e) => {
              e.stopPropagation();
              onAddElement?.();
            }}
          >
            <Plus className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm font-medium">Seção vazia</p>
            <p className="text-xs mt-1">Clique para adicionar elementos</p>
          </div>
        ) : (
          <>
            {section.content.elements.map((element) => (
              <ElementRenderer
                key={element.id}
                element={element}
                device={device}
                variables={variables}
                isEditing={isEditing}
                isSelected={selectedElementId === element.id}
                onClick={(e) => {
                  e.stopPropagation();
                  onElementClick?.(element.id);
                }}
              />
            ))}

            {/* Botão flutuante para adicionar mais elementos */}
            {isEditing && isSelected && (
              <div className="flex justify-center mt-4">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onAddElement?.();
                  }}
                  className="flex items-center gap-2 px-4 py-2 bg-white dark:bg-neutral-800 border rounded-full shadow-md hover:shadow-lg hover:border-primary transition-all text-sm text-muted-foreground hover:text-primary"
                >
                  <Plus size={16} />
                  Adicionar Elemento
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}

// === Preview de Seção (versão simplificada para sidebar) ===

interface SectionPreviewProps {
  section: LandingPageSection;
  onClick?: () => void;
  isSelected?: boolean;
}

export function SectionPreview({ section, onClick, isSelected }: SectionPreviewProps) {
  const sectionTypeLabels: Record<string, string> = {
    HEADER: 'Menu',
    HERO: 'Apresentação',
    ABOUT: 'Sobre',
    SERVICES: 'Serviços',
    PRODUCTS: 'Produtos',
    TEAM: 'Equipe',
    TESTIMONIALS: 'Depoimentos',
    PRICING: 'Planos',
    GALLERY: 'Galeria',
    BOOKING: 'Agendamento',
    CONTACT: 'Contato',
    FAQ: 'FAQ',
    CTA: 'Chamada para Ação',
    FOOTER: 'Rodapé',
    CUSTOM: 'Personalizado',
  };

  return (
    <div
      className={cn(
        'flex items-center gap-3 px-3 py-2 rounded-lg cursor-pointer transition-all',
        'hover:bg-secondary',
        isSelected && 'bg-primary/10 text-primary',
        !section.visible && 'opacity-50'
      )}
      onClick={onClick}
    >
      <GripVertical size={16} className="text-muted-foreground cursor-grab" />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate">
          {section.nome || sectionTypeLabels[section.tipo] || section.tipo}
        </p>
        <p className="text-xs text-muted-foreground capitalize">
          {sectionTypeLabels[section.tipo] || section.tipo}
        </p>
      </div>
      {!section.visible && (
        <EyeOff size={14} className="text-muted-foreground" />
      )}
    </div>
  );
}
