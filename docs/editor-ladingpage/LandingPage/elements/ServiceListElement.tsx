import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import { cn } from '../../../../../lib/utils';

interface ServiceListElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

export function ServiceListElement({
  element,
  styles,
}: ServiceListElementProps) {
  const props = element.props || {};
  const layout = props.layout || 'grid';
  const columns = props.columns || 3;

  const gridClasses = cn(
    layout === 'grid' && `grid gap-4`,
    layout === 'grid' && columns === 2 && 'grid-cols-1 md:grid-cols-2',
    layout === 'grid' && columns === 3 && 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3',
    layout === 'grid' && columns === 4 && 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
    layout === 'list' && 'flex flex-col gap-4',
    layout === 'carousel' && 'flex overflow-x-auto gap-4 pb-4'
  );

  // Placeholder para visualização no editor
  return (
    <div className={gridClasses} style={styles}>
      {[1, 2, 3].map((i) => (
        <div
          key={i}
          className={cn(
            'bg-card border rounded-xl p-4 shadow-sm',
            layout === 'carousel' && 'min-w-[280px]'
          )}
        >
          <div className="w-full h-32 bg-gray-200 rounded-lg mb-4" />
          <h4 className="font-semibold mb-2">Serviço {i}</h4>
          <p className="text-sm text-muted-foreground mb-3">
            Descrição do serviço vai aqui
          </p>
          <div className="flex items-center justify-between">
            {props.showPrice && (
              <span className="text-primary font-bold">R$ 99,00</span>
            )}
            {props.showDuration && (
              <span className="text-sm text-muted-foreground">45 min</span>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
