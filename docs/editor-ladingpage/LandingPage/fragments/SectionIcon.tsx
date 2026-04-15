import {
  LayoutDashboard,
  Home,
  Briefcase,
  ShoppingBag,
  CreditCard,
  Users,
  ShoppingCart,
  Menu,
  ArrowDown,
  Star,
  MessageSquare,
  Calendar,
  Info,
  FileText,
} from 'lucide-react';

interface SectionIconProps {
  sectionId: string;
  className?: string;
  style?: React.CSSProperties;
}

const iconMap: Record<string, typeof Home> = {
  default: LayoutDashboard,
  home: Home,
  services: Briefcase,
  products: ShoppingBag,
  plans: CreditCard,
  about: Users,
  carShop: ShoppingCart,
  header: Menu,
  footer: ArrowDown,
  hero: Star,
  service: Briefcase,
  plan: CreditCard,
  depoiment: MessageSquare,
  agenda: Calendar,
  info: Info,
};

export const SectionIcon = ({ sectionId, className = 'w-4 h-4', style}: SectionIconProps) => {
  const IconComponent = iconMap[sectionId] || FileText;
  return <IconComponent className={className} style={style} />;
};
