import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import { parseVariables } from '../../../../../hooks/useResponsiveStyles';

interface HeadingElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

export function HeadingElement({
  element,
  variables = {},
  styles,
}: HeadingElementProps) {
  const Tag = element.tag || 'h2';
  const content = parseVariables(element.content, variables);

  return (
    <Tag style={styles} className="transition-all">
      {content}
    </Tag>
  );
}
