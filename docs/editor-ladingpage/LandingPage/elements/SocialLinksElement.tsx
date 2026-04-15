import type { LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import { Instagram, Facebook, Youtube, Linkedin, Twitter } from 'lucide-react';

interface SocialLinksElementProps {
  element: LandingPageElement;
  device: DeviceType;
  variables?: Record<string, string>;
  styles: React.CSSProperties;
  isSelected?: boolean;
}

const socialIcons: Record<string, React.ComponentType<{ size?: number; className?: string }>> = {
  instagram: Instagram,
  facebook: Facebook,
  youtube: Youtube,
  linkedin: Linkedin,
  twitter: Twitter,
};

export function SocialLinksElement({
  styles,
}: SocialLinksElementProps) {
  // Por enquanto, mostra ícones de exemplo
  const socialLinks = [
    { platform: 'instagram', url: '#' },
    { platform: 'facebook', url: '#' },
    { platform: 'youtube', url: '#' },
  ];

  return (
    <div className="flex items-center gap-3" style={styles}>
      {socialLinks.map((link) => {
        const Icon = socialIcons[link.platform];
        if (!Icon) return null;

        return (
          <a
            key={link.platform}
            href={link.url}
            target="_blank"
            rel="noopener noreferrer"
            className="p-2 rounded-full bg-primary/10 text-primary hover:bg-primary hover:text-white transition-all"
          >
            <Icon size={20} />
          </a>
        );
      })}
    </div>
  );
}
