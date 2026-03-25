import { cn } from '../../../../../lib/utils';
import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import { useResponsiveStyles, parseVariables } from '../../../../../hooks/useResponsiveStyles';
import { HeadingElement } from './HeadingElement';
import { ParagraphElement } from './ParagraphElement';
import { ButtonElement } from './ButtonElement';
import { ImageElement } from './ImageElement';
import { ContainerElement } from './ContainerElement';
import { SpacerElement } from './SpacerElement';
import { DividerElement } from './DividerElement';
import { IconElement } from './IconElement';
import { ServiceListElement } from './ServiceListElement';
import { SocialLinksElement } from './SocialLinksElement';

interface ElementRendererProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  isSelected?: boolean;
  isEditing?: boolean;
  onClick?: (e: React.MouseEvent) => void;
  onDoubleClick?: (e: React.MouseEvent) => void;
  className?: string;
}

export function ElementRenderer({
  element,
  device,
  variables = {},
  isSelected,
  isEditing,
  onClick,
  onDoubleClick,
  className,
}: ElementRendererProps) {
  const styles = useResponsiveStyles(element.styles, device);

  // Se o elemento está oculto, não renderiza
  if (element.visible === false) {
    return null;
  }

  // Wrapper com indicador de seleção
  const wrapperClassName = cn(
    'relative transition-all duration-200',
    isSelected && 'ring-2 ring-primary ring-offset-2',
    isEditing && 'ring-2 ring-blue-500',
    className,
    element.className
  );

  const commonProps = {
    element,
    device,
    variables,
    styles,
    isSelected,
  };

  // Renderiza baseado no tipo
  switch (element.type) {
    // Texto
    case 'heading':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <HeadingElement {...commonProps} />
        </div>
      );

    case 'paragraph':
    case 'text':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <ParagraphElement {...commonProps} />
        </div>
      );

    case 'rich-text':
      return (
        <div
          className={wrapperClassName}
          onClick={onClick}
          onDoubleClick={onDoubleClick}
          style={styles}
          dangerouslySetInnerHTML={{
            __html: parseVariables(element.content, variables),
          }}
        />
      );

    case 'quote':
      return (
        <blockquote
          className={cn(wrapperClassName, 'border-l-4 border-primary pl-4 italic')}
          onClick={onClick}
          onDoubleClick={onDoubleClick}
          style={styles}
        >
          {parseVariables(element.content, variables)}
        </blockquote>
      );

    case 'list':
      return (
        <ul
          className={cn(wrapperClassName, 'list-disc pl-6 space-y-2')}
          onClick={onClick}
          onDoubleClick={onDoubleClick}
          style={styles}
        >
          {(element.content || '').split('\n').map((item, index) => (
            <li key={index}>{parseVariables(item, variables)}</li>
          ))}
        </ul>
      );

    // Mídia
    case 'image':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <ImageElement {...commonProps} />
        </div>
      );

    case 'video':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <video
            src={element.url}
            controls
            style={styles}
            className="w-full rounded-lg"
          >
            Seu navegador não suporta vídeos.
          </video>
        </div>
      );

    case 'icon':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <IconElement {...commonProps} />
        </div>
      );

    case 'avatar':
    case 'logo':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <ImageElement {...commonProps} className={element.type === 'avatar' ? 'rounded-full' : ''} />
        </div>
      );

    // Interativos
    case 'button':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <ButtonElement {...commonProps} />
        </div>
      );

    case 'link':
      return (
        <a
          href={element.action?.href || '#'}
          target={element.action?.target}
          className={cn(wrapperClassName, 'text-primary hover:underline')}
          onClick={(e) => {
            if (isEditing) {
              e.preventDefault();
            }
            onClick?.(e);
          }}
          onDoubleClick={onDoubleClick}
          style={styles}
        >
          {parseVariables(element.content, variables)}
        </a>
      );

    // Layout
    case 'container':
    case 'row':
    case 'column':
    case 'grid':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <ContainerElement {...commonProps} />
        </div>
      );

    case 'spacer':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <SpacerElement {...commonProps} />
        </div>
      );

    case 'divider':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <DividerElement {...commonProps} />
        </div>
      );

    // Cards
    case 'card':
    case 'service-card':
    case 'product-card':
    case 'team-card':
    case 'testimonial-card':
    case 'pricing-card':
      return (
        <div
          className={cn(wrapperClassName, 'bg-card border rounded-xl p-4 shadow-sm')}
          onClick={onClick}
          onDoubleClick={onDoubleClick}
          style={styles}
        >
          {element.children?.map((child) => (
            <ElementRenderer
              key={child.id}
              element={child}
              device={device}
              variables={variables}
              isSelected={false}
            />
          ))}
        </div>
      );

    // Dados Dinâmicos
    case 'service-list':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <ServiceListElement {...commonProps} />
        </div>
      );

    case 'product-list':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <div className="text-center p-8 border-2 border-dashed rounded-xl text-muted-foreground">
              Lista de Produtos
              <br />
              <span className="text-xs">(Dados dinâmicos)</span>
            </div>
          </div>
        </div>
      );

    case 'team-list':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <div className="text-center p-8 border-2 border-dashed rounded-xl text-muted-foreground">
              Lista da Equipe
              <br />
              <span className="text-xs">(Dados dinâmicos)</span>
            </div>
          </div>
        </div>
      );

    case 'booking-form':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <div className="p-8 border-2 border-dashed rounded-xl text-center text-muted-foreground">
            Formulário de Agendamento
            <br />
            <span className="text-xs">(Componente dinâmico)</span>
          </div>
        </div>
      );

    case 'contact-form':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <div className="p-8 border-2 border-dashed rounded-xl text-center text-muted-foreground">
            Formulário de Contato
            <br />
            <span className="text-xs">(Componente dinâmico)</span>
          </div>
        </div>
      );

    // Widgets
    case 'social-links':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <SocialLinksElement {...commonProps} />
        </div>
      );

    case 'rating':
      return (
        <div
          className={cn(wrapperClassName, 'flex items-center gap-1')}
          onClick={onClick}
          onDoubleClick={onDoubleClick}
          style={styles}
        >
          {[1, 2, 3, 4, 5].map((star) => (
            <span key={star} className="text-yellow-400 text-xl">
              ★
            </span>
          ))}
        </div>
      );

    case 'counter':
      return (
        <div
          className={cn(wrapperClassName, 'text-center')}
          onClick={onClick}
          onDoubleClick={onDoubleClick}
          style={styles}
        >
          <span className="text-4xl font-bold text-primary">
            {element.content || '0'}
          </span>
        </div>
      );

    case 'map':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <div
            className="bg-gray-200 rounded-xl flex items-center justify-center text-muted-foreground"
            style={{ ...styles, minHeight: '200px' }}
          >
            Mapa (Google Maps)
          </div>
        </div>
      );

    case 'whatsapp-button':
      return (
        <div className={wrapperClassName} onClick={onClick} onDoubleClick={onDoubleClick}>
          <button
            className="bg-green-500 hover:bg-green-600 text-white px-6 py-3 rounded-full flex items-center gap-2 shadow-lg transition-all"
            style={styles}
          >
            <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
              <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
            </svg>
            {parseVariables(element.content || 'WhatsApp', variables)}
          </button>
        </div>
      );

    default:
      return (
        <div
          className={cn(wrapperClassName, 'p-4 border-2 border-dashed rounded-lg text-muted-foreground text-center')}
          onClick={onClick}
          onDoubleClick={onDoubleClick}
        >
          Elemento: {element.type}
        </div>
      );
  }
}
