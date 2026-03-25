import { cn } from '../../../../../lib/utils';
import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';

interface ImageElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
  className?: string;
}

export function ImageElement({
  element,
  device,
  styles,
  className,
}: ImageElementProps) {
  // Usa imagem mobile se disponível e o dispositivo for mobile
  const imageUrl = device === 'mobile' && element.urlMobile
    ? element.urlMobile
    : element.url;

  if (!imageUrl) {
    return (
      <div
        className={cn(
          'bg-gray-200 flex items-center justify-center text-muted-foreground',
          className
        )}
        style={{ ...styles, minHeight: '100px' }}
      >
        Imagem não definida
      </div>
    );
  }

  return (
    <img
      src={imageUrl}
      alt={element.alt || 'Imagem'}
      className={cn('transition-all', className)}
      style={styles}
      loading="lazy"
    />
  );
}
