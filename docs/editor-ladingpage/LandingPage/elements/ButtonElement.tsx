import { cn } from '../../../../../lib/utils';
import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import { parseVariables } from '../../../../../hooks/useResponsiveStyles';
import * as LucideIcons from 'lucide-react';

interface ButtonElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

const variantClasses = {
  primary: 'bg-primary text-white hover:bg-primary/90',
  secondary: 'bg-secondary text-secondary-foreground hover:bg-secondary/80',
  outline: 'border-2 border-primary text-primary hover:bg-primary hover:text-white',
  ghost: 'text-primary hover:bg-primary/10',
  link: 'text-primary underline-offset-4 hover:underline',
};

const sizeClasses = {
  xs: 'px-2 py-1 text-xs',
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-6 py-3 text-lg',
  xl: 'px-8 py-4 text-xl',
};

export function ButtonElement({
  element,
  variables = {},
  styles,
}: ButtonElementProps) {
  const content = parseVariables(element.content, variables);
  const variant = element.variant || 'primary';
  const size = element.size || 'md';

  // Busca o ícone do Lucide
  const IconComponent = element.icon?.name
    ? (LucideIcons as unknown as Record<string, React.ComponentType<{ size?: number }>>)[
        element.icon.name.charAt(0).toUpperCase() + element.icon.name.slice(1)
      ] || null
    : null;

  const handleClick = () => {
    if (!element.action) return;

    switch (element.action.type) {
      case 'scroll':
        if (element.action.href) {
          const targetElement = document.querySelector(element.action.href);
          targetElement?.scrollIntoView({ behavior: 'smooth' });
        }
        break;
      case 'link':
        if (element.action.href) {
          if (element.action.target === '_blank') {
            window.open(element.action.href, '_blank');
          } else {
            window.location.href = element.action.href;
          }
        }
        break;
      case 'whatsapp':
        if (element.action.href) {
          window.open(`https://wa.me/${element.action.href.replace(/\D/g, '')}`, '_blank');
        }
        break;
      case 'phone':
        if (element.action.href) {
          window.location.href = `tel:${element.action.href}`;
        }
        break;
      case 'email':
        if (element.action.href) {
          window.location.href = `mailto:${element.action.href}`;
        }
        break;
      default:
        break;
    }
  };

  return (
    <button
      onClick={handleClick}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-all duration-200',
        variantClasses[variant],
        sizeClasses[size]
      )}
      style={styles}
    >
      {IconComponent && element.icon?.position === 'left' && (
        <IconComponent size={element.icon.size || 16} />
      )}
      {content}
      {IconComponent && element.icon?.position === 'right' && (
        <IconComponent size={element.icon.size || 16} />
      )}
    </button>
  );
}
