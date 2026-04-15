import { useState } from 'react';
import {
  Type,
  Image,
  Square,
  MousePointer,
  Link,
  Grid3X3,
  Minus,
  Space,
  Quote,
  List,
  Video,
  Star,
  Users,
  CreditCard,
  MessageSquare,
  ShoppingBag,
  Calendar,
  Mail,
  Share2,
  MapPin,
  Phone,
  X,
  Search,
  LucideIcon,
} from 'lucide-react';
import { cn } from '../../../../../lib/utils';
import type { ElementType, LandingPageElement } from '../../../../../types/landing-page.types';

interface AddElementPanelProps {
  onAddElement: (element: Partial<LandingPageElement>) => void;
  onClose: () => void;
}

interface ElementOption {
  type: ElementType;
  label: string;
  description: string;
  icon: LucideIcon;
  category: string;
  defaultProps: Partial<LandingPageElement>;
}

const elementOptions: ElementOption[] = [
  // Texto
  {
    type: 'heading',
    label: 'Título',
    description: 'Títulos H1 a H6',
    icon: Type,
    category: 'Texto',
    defaultProps: {
      tag: 'h2',
      content: 'Novo Título',
      styles: {
        desktop: { fontSize: '32px', fontWeight: 'bold' },
      },
    },
  },
  {
    type: 'paragraph',
    label: 'Parágrafo',
    description: 'Bloco de texto',
    icon: Type,
    category: 'Texto',
    defaultProps: {
      content: 'Digite seu texto aqui...',
      styles: {
        desktop: { fontSize: '16px', lineHeight: '1.6' },
      },
    },
  },
  {
    type: 'text',
    label: 'Texto Inline',
    description: 'Texto simples',
    icon: Type,
    category: 'Texto',
    defaultProps: {
      content: 'Texto',
    },
  },
  {
    type: 'quote',
    label: 'Citação',
    description: 'Bloco de citação',
    icon: Quote,
    category: 'Texto',
    defaultProps: {
      content: '"Uma citação inspiradora..."',
      styles: {
        desktop: { fontStyle: 'italic', borderLeft: '4px solid', paddingLeft: '16px' },
      },
    },
  },
  {
    type: 'list',
    label: 'Lista',
    description: 'Lista de itens',
    icon: List,
    category: 'Texto',
    defaultProps: {
      content: 'Item 1\nItem 2\nItem 3',
    },
  },

  // Mídia
  {
    type: 'image',
    label: 'Imagem',
    description: 'Imagem com alt text',
    icon: Image,
    category: 'Mídia',
    defaultProps: {
      url: 'https://via.placeholder.com/600x400',
      alt: 'Descrição da imagem',
      styles: {
        desktop: { width: '100%', borderRadius: '8px' },
      },
    },
  },
  {
    type: 'video',
    label: 'Vídeo',
    description: 'Vídeo incorporado',
    icon: Video,
    category: 'Mídia',
    defaultProps: {
      url: '',
      styles: {
        desktop: { width: '100%', borderRadius: '8px' },
      },
    },
  },
  {
    type: 'icon',
    label: 'Ícone',
    description: 'Ícone decorativo',
    icon: Star,
    category: 'Mídia',
    defaultProps: {
      icon: { name: 'star', size: 24 },
    },
  },
  {
    type: 'avatar',
    label: 'Avatar',
    description: 'Foto de perfil circular',
    icon: Users,
    category: 'Mídia',
    defaultProps: {
      url: 'https://via.placeholder.com/100x100',
      alt: 'Avatar',
      styles: {
        desktop: { width: '80px', height: '80px', borderRadius: '50%' },
      },
    },
  },

  // Interativos
  {
    type: 'button',
    label: 'Botão',
    description: 'Botão de ação',
    icon: MousePointer,
    category: 'Interativos',
    defaultProps: {
      content: 'Clique Aqui',
      variant: 'primary',
      size: 'md',
      action: { type: 'link', href: '#' },
    },
  },
  {
    type: 'link',
    label: 'Link',
    description: 'Link de texto',
    icon: Link,
    category: 'Interativos',
    defaultProps: {
      content: 'Saiba mais',
      action: { type: 'link', href: '#', target: '_self' },
    },
  },
  {
    type: 'whatsapp-button',
    label: 'Botão WhatsApp',
    description: 'Botão de WhatsApp',
    icon: Phone,
    category: 'Interativos',
    defaultProps: {
      content: 'Fale Conosco',
      action: { type: 'whatsapp', href: '{whatsapp}' },
    },
  },

  // Layout
  {
    type: 'container',
    label: 'Container',
    description: 'Agrupa elementos',
    icon: Square,
    category: 'Layout',
    defaultProps: {
      children: [],
      styles: {
        desktop: { display: 'flex', flexDirection: 'column', gap: '16px' },
      },
    },
  },
  {
    type: 'row',
    label: 'Linha',
    description: 'Elementos em linha',
    icon: Grid3X3,
    category: 'Layout',
    defaultProps: {
      children: [],
      styles: {
        desktop: { display: 'flex', flexDirection: 'row', gap: '16px' },
        mobile: { flexDirection: 'column' },
      },
    },
  },
  {
    type: 'grid',
    label: 'Grade',
    description: 'Layout em grade',
    icon: Grid3X3,
    category: 'Layout',
    defaultProps: {
      children: [],
      styles: {
        desktop: { display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px' },
        tablet: { gridTemplateColumns: 'repeat(2, 1fr)' },
        mobile: { gridTemplateColumns: '1fr' },
      },
    },
  },
  {
    type: 'spacer',
    label: 'Espaçador',
    description: 'Espaço vertical',
    icon: Space,
    category: 'Layout',
    defaultProps: {
      styles: {
        desktop: { height: '40px' },
        mobile: { height: '24px' },
      },
    },
  },
  {
    type: 'divider',
    label: 'Divisor',
    description: 'Linha divisória',
    icon: Minus,
    category: 'Layout',
    defaultProps: {
      styles: {
        desktop: { borderTop: '1px solid #e5e7eb', margin: '24px 0' },
      },
    },
  },

  // Cards
  {
    type: 'card',
    label: 'Card',
    description: 'Card genérico',
    icon: Square,
    category: 'Cards',
    defaultProps: {
      children: [],
      styles: {
        desktop: { padding: '24px', borderRadius: '12px', border: '1px solid #e5e7eb' },
      },
    },
  },
  {
    type: 'service-card',
    label: 'Card de Serviço',
    description: 'Card para serviço',
    icon: Star,
    category: 'Cards',
    defaultProps: {
      children: [],
    },
  },
  {
    type: 'testimonial-card',
    label: 'Card de Depoimento',
    description: 'Card para depoimento',
    icon: MessageSquare,
    category: 'Cards',
    defaultProps: {
      children: [],
    },
  },
  {
    type: 'pricing-card',
    label: 'Card de Preço',
    description: 'Card para planos',
    icon: CreditCard,
    category: 'Cards',
    defaultProps: {
      children: [],
    },
  },

  // Dados Dinâmicos
  {
    type: 'service-list',
    label: 'Lista de Serviços',
    description: 'Serviços da API',
    icon: Grid3X3,
    category: 'Dinâmicos',
    defaultProps: {
      props: {
        layout: 'grid',
        columns: 3,
        showPrice: true,
        showDuration: true,
      },
    },
  },
  {
    type: 'product-list',
    label: 'Lista de Produtos',
    description: 'Produtos da API',
    icon: ShoppingBag,
    category: 'Dinâmicos',
    defaultProps: {
      props: {
        layout: 'grid',
        columns: 4,
        showPrice: true,
      },
    },
  },
  {
    type: 'team-list',
    label: 'Lista de Equipe',
    description: 'Membros da equipe',
    icon: Users,
    category: 'Dinâmicos',
    defaultProps: {
      props: {
        layout: 'grid',
        columns: 4,
      },
    },
  },
  {
    type: 'booking-form',
    label: 'Agendamento',
    description: 'Formulário de agendamento',
    icon: Calendar,
    category: 'Dinâmicos',
    defaultProps: {},
  },
  {
    type: 'contact-form',
    label: 'Formulário de Contato',
    description: 'Formulário de contato',
    icon: Mail,
    category: 'Dinâmicos',
    defaultProps: {},
  },

  // Widgets
  {
    type: 'social-links',
    label: 'Redes Sociais',
    description: 'Links das redes',
    icon: Share2,
    category: 'Widgets',
    defaultProps: {},
  },
  {
    type: 'rating',
    label: 'Avaliação',
    description: 'Estrelas de avaliação',
    icon: Star,
    category: 'Widgets',
    defaultProps: {
      content: '5',
    },
  },
  {
    type: 'counter',
    label: 'Contador',
    description: 'Contador animado',
    icon: Type,
    category: 'Widgets',
    defaultProps: {
      content: '100',
    },
  },
  {
    type: 'map',
    label: 'Mapa',
    description: 'Google Maps',
    icon: MapPin,
    category: 'Widgets',
    defaultProps: {},
  },
];

const categories = ['Texto', 'Mídia', 'Interativos', 'Layout', 'Cards', 'Dinâmicos', 'Widgets'];

export function AddElementPanel({ onAddElement, onClose }: AddElementPanelProps) {
  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  const filteredElements = elementOptions.filter((element) => {
    const matchesSearch =
      element.label.toLowerCase().includes(search.toLowerCase()) ||
      element.description.toLowerCase().includes(search.toLowerCase());
    const matchesCategory = !selectedCategory || element.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const groupedElements = categories.reduce((acc, category) => {
    const elements = filteredElements.filter((e) => e.category === category);
    if (elements.length > 0) {
      acc[category] = elements;
    }
    return acc;
  }, {} as Record<string, ElementOption[]>);

  const handleAddElement = (option: ElementOption) => {
    const newElement: Partial<LandingPageElement> = {
      type: option.type,
      ...option.defaultProps,
    };
    onAddElement(newElement);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      <div className="relative bg-white dark:bg-neutral-800 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <div>
            <h3 className="text-lg font-semibold">Adicionar Elemento</h3>
            <p className="text-sm text-muted-foreground">Escolha um elemento para adicionar à seção</p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-secondary rounded-lg">
            <X size={18} />
          </button>
        </div>

        {/* Search */}
        <div className="px-4 py-3 border-b">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              type="text"
              placeholder="Buscar elemento..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              autoFocus
            />
          </div>

          {/* Category Filters */}
          <div className="flex flex-wrap gap-1.5 mt-3">
            <button
              onClick={() => setSelectedCategory(null)}
              className={cn(
                'px-3 py-1 rounded-full text-xs font-medium transition-colors',
                !selectedCategory
                  ? 'bg-primary text-white'
                  : 'bg-secondary text-muted-foreground hover:text-foreground'
              )}
            >
              Todos
            </button>
            {categories.map((category) => (
              <button
                key={category}
                onClick={() => setSelectedCategory(category)}
                className={cn(
                  'px-3 py-1 rounded-full text-xs font-medium transition-colors',
                  selectedCategory === category
                    ? 'bg-primary text-white'
                    : 'bg-secondary text-muted-foreground hover:text-foreground'
                )}
              >
                {category}
              </button>
            ))}
          </div>
        </div>

        {/* Elements Grid */}
        <div className="flex-1 overflow-y-auto p-4">
          {Object.entries(groupedElements).map(([category, elements]) => (
            <div key={category} className="mb-6">
              <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                {category}
              </h4>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {elements.map((element) => (
                  <button
                    key={element.type}
                    onClick={() => handleAddElement(element)}
                    className="flex items-start gap-3 p-3 border rounded-xl hover:border-primary hover:bg-primary/5 transition-all text-left group"
                  >
                    <div className="w-9 h-9 rounded-lg bg-secondary flex items-center justify-center flex-shrink-0 group-hover:bg-primary/10">
                      <element.icon size={18} className="text-muted-foreground group-hover:text-primary" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-sm font-medium truncate">{element.label}</p>
                      <p className="text-xs text-muted-foreground truncate">{element.description}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          ))}

          {Object.keys(groupedElements).length === 0 && (
            <div className="text-center py-12 text-muted-foreground">
              <p>Nenhum elemento encontrado</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
