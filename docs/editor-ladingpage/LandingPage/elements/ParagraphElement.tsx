import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import { parseVariables } from '../../../../../hooks/useResponsiveStyles';

interface ParagraphElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

export function ParagraphElement({
  element,
  variables = {},
  styles,
}: ParagraphElementProps) {
  const content = parseVariables(element.content, variables);

  return (
    <p style={styles} className="transition-all">
      {content}
    </p>
  );
}
