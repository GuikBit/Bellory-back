import { useState } from 'react';
import {
  Plus,
  GripVertical,
  Eye,
  EyeOff,
  Copy,
  Trash2,
  Layout,
  Type,
  Image,
  Users,
  Star,
  CreditCard,
  Grid,
  Calendar,
  MessageSquare,
  HelpCircle,
  Mail,
  Sparkles,
  Menu,
  ArrowDown,
  LucideIcon,
} from 'lucide-react';
import { cn } from '../../../../../lib/utils';
import type { LandingPageSection, SectionType } from '../../../../../types/landing-page.types';
import { BarbeariaButton } from '../../../../ui';
import { useTheme } from '../../../../../global/Theme-context';

interface SectionListSidebarProps {
  sections: LandingPageSection[];
  selectedSectionId: string | null;
  onSelectSection: (sectionId: string) => void;
  onAddSection: (tipo: SectionType) => void;
  onDuplicateSection: (sectionId: string) => void;
  onDeleteSection: (sectionId: string) => void;
  onToggleVisibility: (sectionId: string) => void;
  onReorderSections?: (sectionIds: string[]) => void;
  isMobile?: boolean;
  onClose?: () => void;
}

// Metadados dos tipos de seção
const sectionTypeMetadata: {
  type: SectionType;
  label: string;
  description: string;
  icon: LucideIcon;
}[] = [
  { type: 'HEADER', label: 'Menu', description: 'Barra de navegação', icon: Menu },
  { type: 'HERO', label: 'Apresentação', description: 'Banner principal', icon: Star },
  { type: 'ABOUT', label: 'Sobre', description: 'Sobre a empresa', icon: Users },
  { type: 'SERVICES', label: 'Serviços', description: 'Lista de serviços', icon: Grid },
  { type: 'PRODUCTS', label: 'Produtos', description: 'Lista de produtos', icon: Image },
  { type: 'TEAM', label: 'Equipe', description: 'Membros da equipe', icon: Users },
  { type: 'TESTIMONIALS', label: 'Depoimentos', description: 'Avaliações de clientes', icon: MessageSquare },
  { type: 'PRICING', label: 'Planos', description: 'Tabela de preços', icon: CreditCard },
  { type: 'GALLERY', label: 'Galeria', description: 'Galeria de imagens', icon: Image },
  { type: 'BOOKING', label: 'Agendamento', description: 'Sistema de agendamento', icon: Calendar },
  { type: 'CONTACT', label: 'Contato', description: 'Informações de contato', icon: Mail },
  { type: 'FAQ', label: 'FAQ', description: 'Perguntas frequentes', icon: HelpCircle },
  { type: 'CTA', label: 'Chamada', description: 'Call to action', icon: Type },
  { type: 'FOOTER', label: 'Rodapé', description: 'Rodapé do site', icon: ArrowDown },
  { type: 'CUSTOM', label: 'Personalizado', description: 'Seção customizada', icon: Layout },
];

const sectionTypeLabels: Record<SectionType, string> = {
  HEADER: 'Menu',
  HERO: 'Apresentação',
  ABOUT: 'Sobre',
  SERVICES: 'Serviços',
  PRODUCTS: 'Produtos',
  TEAM: 'Equipe',
  TESTIMONIALS: 'Depoimentos',
  PRICING: 'Planos',
  GALLERY: 'Galeria',
  BOOKING: 'Agendamento',
  CONTACT: 'Contato',
  FAQ: 'FAQ',
  CTA: 'Chamada para Ação',
  FOOTER: 'Rodapé',
  CUSTOM: 'Personalizado',
};

const sectionTypeIcons: Record<SectionType, LucideIcon> = {
  HEADER: Menu,
  HERO: Star,
  ABOUT: Users,
  SERVICES: Grid,
  PRODUCTS: Image,
  TEAM: Users,
  TESTIMONIALS: MessageSquare,
  PRICING: CreditCard,
  GALLERY: Image,
  BOOKING: Calendar,
  CONTACT: Mail,
  FAQ: HelpCircle,
  CTA: Type,
  FOOTER: ArrowDown,
  CUSTOM: Layout,
};

export function SectionListSidebar({
  sections,
  selectedSectionId,
  onSelectSection,
  onAddSection,
  onDuplicateSection,
  onDeleteSection,
  onToggleVisibility,
  isMobile = false,
  onClose,
}: SectionListSidebarProps) {
  const { currentTheme: theme } = useTheme();
  const [showAddModal, setShowAddModal] = useState(false);
  const [_draggedIndex, setDraggedIndex] = useState<number | null>(null);

  // Ordena seções por order
  const orderedSections = [...sections].sort((a, b) => a.order - b.order);

  const handleSectionClick = (sectionId: string) => {
    onSelectSection(sectionId);
    if (isMobile && onClose) {
      onClose();
    }
  };

  const handleAddSection = (tipo: SectionType) => {
    onAddSection(tipo);
    setShowAddModal(false);
  };

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="h-[50px] flex items-center justify-between px-4 py-4 border-b dark:border-neutral-700 border-gray-200">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg gradient-primary flex items-center justify-center">
            <Sparkles className="w-4 h-4 text-primary" />
          </div>
          <div>
            <h2 className="font-semibold text-foreground text-sm">Seções</h2>
            <p className="text-[10px] text-muted-foreground">{sections.length} seções</p>
          </div>
        </div>

        <BarbeariaButton
          variant="outline"
          size="xs"
          leftIcon={<Plus size={14} />}
          onClick={() => setShowAddModal(true)}
          rounded="lg"
        >
          Adicionar
        </BarbeariaButton>
      </div>

      {/* Lista de Seções */}
      <nav className="flex-1 overflow-y-auto scrollbar-thin scrollbar-thumb-rounded-full scrollbar-thumb-primary/30 scrollbar-track-transparent py-3 px-2">
        {orderedSections.length === 0 ? (
          <div className="text-center py-8 px-4">
            <div className="w-12 h-12 mx-auto mb-3 rounded-xl bg-secondary flex items-center justify-center">
              <Layout className="w-6 h-6 text-muted-foreground" />
            </div>
            <p className="text-sm text-muted-foreground">Nenhuma seção adicionada</p>
            <p className="text-xs text-muted-foreground mt-1">
              Clique em "Adicionar" para começar
            </p>
          </div>
        ) : (
          <div className="space-y-1">
            {orderedSections.map((section, index) => {
              const Icon = sectionTypeIcons[section.tipo] || Layout;
              const isSelected = selectedSectionId === section.sectionId;

              return (
                <div
                  key={section.sectionId}
                  className={cn(
                    'group flex items-center gap-2 px-2 py-2 rounded-lg cursor-pointer transition-all',
                    'hover:bg-secondary',
                    isSelected && 'bg-primary/10 ring-1 ring-primary/30',
                    !section.visible && 'opacity-50'
                  )}
                  onClick={() => handleSectionClick(section.sectionId)}
                  draggable
                  onDragStart={() => setDraggedIndex(index)}
                  onDragEnd={() => setDraggedIndex(null)}
                  onDragOver={(e) => e.preventDefault()}
                >
                  {/* Drag Handle */}
                  <div className="cursor-grab opacity-0 group-hover:opacity-100 transition-opacity">
                    <GripVertical size={14} className="text-muted-foreground" />
                  </div>

                  {/* Icon */}
                  <div
                    className={cn(
                      'w-7 h-7 rounded-md flex items-center justify-center flex-shrink-0',
                      isSelected ? 'bg-primary/20' : 'bg-secondary'
                    )}
                  >
                    <Icon
                      size={14}
                      style={{ color: isSelected ? theme.colors.primary : theme.colors.textSecondary }}
                    />
                  </div>

                  {/* Content */}
                  <div className="flex-1 min-w-0">
                    <p
                      className={cn(
                        'text-sm font-medium truncate',
                        isSelected && 'text-primary'
                      )}
                    >
                      {section.nome || sectionTypeLabels[section.tipo]}
                    </p>
                    <p className="text-xs text-muted-foreground truncate">
                      {sectionTypeLabels[section.tipo]}
                    </p>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onToggleVisibility(section.sectionId);
                      }}
                      className="p-1 hover:bg-background rounded"
                      title={section.visible ? 'Ocultar' : 'Mostrar'}
                    >
                      {section.visible ? (
                        <Eye size={12} className="text-muted-foreground" />
                      ) : (
                        <EyeOff size={12} className="text-muted-foreground" />
                      )}
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onDuplicateSection(section.sectionId);
                      }}
                      className="p-1 hover:bg-background rounded"
                      title="Duplicar"
                    >
                      <Copy size={12} className="text-muted-foreground" />
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onDeleteSection(section.sectionId);
                      }}
                      className="p-1 hover:bg-background rounded text-red-500"
                      title="Excluir"
                    >
                      <Trash2 size={12} />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </nav>

      {/* Modal para adicionar seção */}
      {showAddModal && (
        <AddSectionModal
          onAdd={handleAddSection}
          onClose={() => setShowAddModal(false)}
        />
      )}
    </div>
  );
}

// === Modal para Adicionar Seção ===

interface AddSectionModalProps {
  onAdd: (tipo: SectionType) => void;
  onClose: () => void;
}

function AddSectionModal({ onAdd, onClose }: AddSectionModalProps) {
  const [search, setSearch] = useState('');

  const filteredTypes = sectionTypeMetadata.filter(
    (item) =>
      item.label.toLowerCase().includes(search.toLowerCase()) ||
      item.description.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Overlay */}
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative bg-white dark:bg-neutral-800 rounded-2xl shadow-2xl w-full max-w-md max-h-[80vh] flex flex-col">
        {/* Header */}
        <div className="px-4 py-3 border-b dark:border-neutral-700">
          <h3 className="text-lg font-semibold">Adicionar Seção</h3>
          <p className="text-sm text-muted-foreground">Escolha um tipo de seção para adicionar</p>
        </div>

        {/* Search */}
        <div className="px-4 py-3 border-b dark:border-neutral-700">
          <input
            type="text"
            placeholder="Buscar seção..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            autoFocus
          />
        </div>

        {/* List */}
        <div className="flex-1 overflow-y-auto p-2">
          <div className="grid grid-cols-2 gap-2">
            {filteredTypes.map((item) => (
              <button
                key={item.type}
                onClick={() => onAdd(item.type)}
                className="flex flex-col items-center gap-2 p-4 rounded-xl border hover:border-primary hover:bg-primary/5 transition-all text-center"
              >
                <div className="w-10 h-10 rounded-xl bg-secondary flex items-center justify-center">
                  <item.icon size={20} className="text-muted-foreground" />
                </div>
                <div>
                  <p className="text-sm font-medium">{item.label}</p>
                  <p className="text-xs text-muted-foreground">{item.description}</p>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="px-4 py-3 border-t dark:border-neutral-700 flex justify-end">
          <BarbeariaButton variant="ghost" onClick={onClose}>
            Cancelar
          </BarbeariaButton>
        </div>
      </div>
    </div>
  );
}
