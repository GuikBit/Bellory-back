import { ReactNode } from 'react';
import { cn } from '../../../../../lib/utils';
import type { DeviceType } from '../../../../../types/landing-page.types';

interface DevicePreviewProps {
  device: DeviceType;
  children: ReactNode;
  zoom?: number;
  className?: string;
  showFrame?: boolean;
}

const DEVICE_CONFIGS = {
  desktop: {
    width: '100%',
    maxWidth: '100%',
    height: '100%',
    frame: null,
  },
  tablet: {
    width: '768px',
    maxWidth: '768px',
    height: '100%',
    frame: {
      borderRadius: '24px',
      padding: '20px',
    },
  },
  mobile: {
    width: '375px',
    maxWidth: '375px',
    height: '100%',
    frame: {
      borderRadius: '36px',
      padding: '12px',
    },
  },
};

export function DevicePreview({
  device,
  children,
  zoom = 100,
  className,
  showFrame = true,
}: DevicePreviewProps) {
  const config = DEVICE_CONFIGS[device];

  return (
    <div className={cn('flex justify-center items-start h-full p-4 bg-[hsl(var(--editor-preview))]', className)}>
      <div
        className={cn(
          'relative transition-all duration-300 ease-out',
          device !== 'desktop' && showFrame && 'bg-neutral-800 shadow-2xl'
        )}
        style={{
          width: config.width,
          maxWidth: config.maxWidth,
          transform: zoom !== 100 ? `scale(${zoom / 100})` : undefined,
          transformOrigin: 'top center',
          borderRadius: config.frame?.borderRadius,
          padding: showFrame ? config.frame?.padding : undefined,
        }}
      >
        {/* Notch para mobile */}
        {device === 'mobile' && showFrame && (
          <div className="absolute top-2 left-1/2 -translate-x-1/2 w-24 h-6 bg-neutral-900 rounded-b-xl z-10" />
        )}

        {/* Frame do dispositivo */}
        <div
          className={cn(
            'bg-white dark:bg-neutral-900 overflow-hidden h-full',
            device !== 'desktop' && showFrame && 'rounded-2xl',
            device === 'mobile' && showFrame && 'rounded-[28px]'
          )}
        >
          {/* Content Area */}
          <div className="h-full overflow-y-auto scrollbar-thin scrollbar-thumb-rounded-full scrollbar-thumb-primary/30 scrollbar-track-transparent">
            {children}
          </div>
        </div>

        {/* Home indicator para mobile */}
        {device === 'mobile' && showFrame && (
          <div className="absolute bottom-1 left-1/2 -translate-x-1/2 w-32 h-1 bg-neutral-600 rounded-full" />
        )}
      </div>
    </div>
  );
}

// === Device Frame para Preview Completo ===

interface DeviceFrameProps {
  device: DeviceType;
  children: ReactNode;
  className?: string;
}

export function DeviceFrame({ device, children, className }: DeviceFrameProps) {
  if (device === 'desktop') {
    return (
      <div className={cn('w-full h-full bg-white dark:bg-neutral-900', className)}>
        {children}
      </div>
    );
  }

  if (device === 'tablet') {
    return (
      <div className={cn('relative bg-neutral-800 rounded-3xl p-4 shadow-2xl', className)}>
        {/* Camera */}
        <div className="absolute top-2 left-1/2 -translate-x-1/2 w-3 h-3 bg-neutral-700 rounded-full" />

        {/* Screen */}
        <div className="bg-white dark:bg-neutral-900 rounded-2xl overflow-hidden h-full">
          {children}
        </div>

        {/* Home button */}
        <div className="absolute bottom-1.5 left-1/2 -translate-x-1/2 w-10 h-1 bg-neutral-600 rounded-full" />
      </div>
    );
  }

  // Mobile
  return (
    <div className={cn('relative bg-neutral-800 rounded-[40px] p-3 shadow-2xl', className)}>
      {/* Notch */}
      <div className="absolute top-3 left-1/2 -translate-x-1/2 w-28 h-7 bg-neutral-900 rounded-b-2xl z-10 flex items-center justify-center gap-2">
        <div className="w-2 h-2 bg-neutral-700 rounded-full" />
        <div className="w-12 h-3 bg-neutral-700 rounded-full" />
      </div>

      {/* Screen */}
      <div className="bg-white dark:bg-neutral-900 rounded-[32px] overflow-hidden h-full pt-6">
        {children}
      </div>

      {/* Home indicator */}
      <div className="absolute bottom-2 left-1/2 -translate-x-1/2 w-36 h-1.5 bg-neutral-600 rounded-full" />
    </div>
  );
}

// === Preview Container com Header ===

interface PreviewContainerProps {
  title?: string;
  subtitle?: string;
  device: DeviceType;
  children: ReactNode;
  className?: string;
}

export function PreviewContainer({
  title,
  subtitle,
  device,
  children,
  className,
}: PreviewContainerProps) {
  return (
    <div className={cn('flex flex-col h-full', className)}>
      {(title || subtitle) && (
        <div className="text-center py-3 border-b">
          {title && <h3 className="text-sm font-medium">{title}</h3>}
          {subtitle && <p className="text-xs text-muted-foreground">{subtitle}</p>}
        </div>
      )}

      <DevicePreview device={device} className="flex-1">
        {children}
      </DevicePreview>
    </div>
  );
}
