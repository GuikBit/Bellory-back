import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import * as LucideIcons from 'lucide-react';

interface IconElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

export function IconElement({
  element,
  styles,
}: IconElementProps) {
  const iconName = element.icon?.name || element.content || 'Star';

  // Converte para PascalCase
  const pascalCaseName = iconName
    .split('-')
    .map((word: string) => word.charAt(0).toUpperCase() + word.slice(1))
    .join('');

  const IconComponent = (LucideIcons as unknown as Record<string, React.ComponentType<{ size?: number; style?: React.CSSProperties }>>)[
    pascalCaseName
  ];

  if (!IconComponent) {
    return (
      <span style={styles} className="text-muted-foreground">
        [Ícone: {iconName}]
      </span>
    );
  }

  return (
    <IconComponent
      size={element.icon?.size || 24}
      style={styles}
    />
  );
}
