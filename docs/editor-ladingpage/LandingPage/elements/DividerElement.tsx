import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';

interface DividerElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

export function DividerElement({ styles }: DividerElementProps) {
  return (
    <hr
      className="border-t border-border"
      style={{
        margin: '16px 0',
        ...styles,
      }}
    />
  );
}
