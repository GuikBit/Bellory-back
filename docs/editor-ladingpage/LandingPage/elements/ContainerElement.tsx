import { cn } from '../../../../../lib/utils';
import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import { ElementRenderer } from './ElementRenderer';

interface ContainerElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

export function ContainerElement({
  element,
  device,
  variables = {},
  styles,
}: ContainerElementProps) {
  const containerClasses = cn(
    'transition-all',
    element.type === 'row' && 'flex flex-row flex-wrap',
    element.type === 'column' && 'flex flex-col',
    element.type === 'grid' && 'grid'
  );

  return (
    <div className={containerClasses} style={styles}>
      {element.children?.map((child) => (
        <ElementRenderer
          key={child.id}
          element={child}
          device={device}
          variables={variables}
        />
      ))}
    </div>
  );
}
