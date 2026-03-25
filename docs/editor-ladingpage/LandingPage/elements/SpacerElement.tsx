import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';

interface SpacerElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

export function SpacerElement({ styles }: SpacerElementProps) {
  return (
    <div
      className="w-full"
      style={{
        height: styles.height || '24px',
        ...styles,
      }}
    />
  );
}
