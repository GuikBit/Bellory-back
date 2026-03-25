import { useState } from 'react';
import { Plus, FileText, Loader2, Trash2, Copy, ExternalLink } from 'lucide-react';
import { LandingPageDTO } from '../../../../types/landing-page.types';
import { useLandingPages, useCreateLandingPage, useDeleteLandingPage, useDuplicateLandingPage } from '../../../../hooks/useLandingPage';
import LandingPageEditor from './LandingPageEditor';
import { BarbeariaButton } from '../../../ui';
import { cn } from '../../../../lib/utils';
import { useAuth } from '../../../../global/AuthContext';

// Dados de demonstração para quando não há API
const demoLandingPage: LandingPageDTO = {
  id: 'demo-1',
  organizacaoId: 1,
  nome: 'Landing Page de Demonstração',
  slug: 'demo-landing',
  tipo: 'HOME',
  status: 'DRAFT',
  currentVersion: 1,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  globalSettings: {
    theme: 'light',
    primaryColor: '#8B5CF6',
  },
  sections: [
    {
      sectionId: 'header-1',
      tipo: 'HEADER',
      nome: 'Menu de Navegação',
      visible: true,
      order: 0,
      content: {
        layout: 'full',
        alignment: 'center',
        elements: [
          {
            id: 'logo-1',
            type: 'logo',
            url: '/logo.png',
            alt: 'Logo da empresa',
            styles: {
              desktop: { height: '40px' },
            },
          },
          {
            id: 'menu-1',
            type: 'menu',
            content: 'Início, Sobre, Serviços, Contato',
          },
        ],
      },
      styles: {
        desktop: { padding: '16px 32px', backgroundColor: '#ffffff' },
      },
    },
    {
      sectionId: 'hero-1',
      tipo: 'HERO',
      nome: 'Banner Principal',
      visible: true,
      order: 1,
      background: {
        type: 'gradient',
        gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      },
      content: {
        layout: 'contained',
        alignment: 'center',
        elements: [
          {
            id: 'title-1',
            type: 'heading',
            tag: 'h1',
            content: 'Bem-vindo à {empresa}',
            styles: {
              desktop: { fontSize: '56px', fontWeight: 'bold', color: '#ffffff', textAlign: 'center' },
              tablet: { fontSize: '42px' },
              mobile: { fontSize: '32px' },
            },
          },
          {
            id: 'subtitle-1',
            type: 'paragraph',
            content: 'Transformamos suas ideias em realidade. Agende agora e descubra a diferença.',
            styles: {
              desktop: { fontSize: '20px', color: '#ffffff', textAlign: 'center', maxWidth: '600px', margin: '0 auto' },
              mobile: { fontSize: '16px' },
            },
          },
          {
            id: 'cta-container',
            type: 'container',
            styles: {
              desktop: { display: 'flex', gap: '16px', justifyContent: 'center', marginTop: '32px' },
              mobile: { flexDirection: 'column', alignItems: 'center' },
            },
            children: [
              {
                id: 'btn-primary',
                type: 'button',
                content: 'Agendar Agora',
                variant: 'primary',
                size: 'lg',
                icon: { name: 'calendar', position: 'left' },
                action: { type: 'scroll', href: '#booking' },
              },
              {
                id: 'btn-secondary',
                type: 'button',
                content: 'Saiba Mais',
                variant: 'outline',
                size: 'lg',
                action: { type: 'scroll', href: '#about' },
              },
            ],
          },
        ],
      },
      styles: {
        desktop: { padding: '120px 32px', minHeight: '600px' },
        tablet: { padding: '80px 24px' },
        mobile: { padding: '60px 16px' },
      },
    },
    {
      sectionId: 'about-1',
      tipo: 'ABOUT',
      nome: 'Sobre Nós',
      visible: true,
      order: 2,
      content: {
        layout: 'contained',
        alignment: 'center',
        elements: [
          {
            id: 'about-title',
            type: 'heading',
            tag: 'h2',
            content: 'Sobre a {empresa}',
            styles: {
              desktop: { fontSize: '42px', fontWeight: 'bold', textAlign: 'center', marginBottom: '24px' },
            },
          },
          {
            id: 'about-text',
            type: 'paragraph',
            content: 'Somos uma empresa dedicada a oferecer os melhores serviços para você. Com anos de experiência no mercado, nossa equipe está pronta para atender suas necessidades.',
            styles: {
              desktop: { fontSize: '18px', textAlign: 'center', maxWidth: '800px', margin: '0 auto', color: '#666' },
            },
          },
        ],
      },
      styles: {
        desktop: { padding: '80px 32px', backgroundColor: '#f9fafb' },
      },
    },
    {
      sectionId: 'services-1',
      tipo: 'SERVICES',
      nome: 'Nossos Serviços',
      visible: true,
      order: 3,
      content: {
        layout: 'contained',
        alignment: 'center',
        elements: [
          {
            id: 'services-title',
            type: 'heading',
            tag: 'h2',
            content: 'Nossos Serviços',
            styles: {
              desktop: { fontSize: '42px', fontWeight: 'bold', textAlign: 'center', marginBottom: '48px' },
            },
          },
          {
            id: 'services-list',
            type: 'service-list',
            props: {
              layout: 'grid',
              columns: 3,
              showPrice: true,
              showDuration: true,
              cardVariant: 'default',
            },
          },
        ],
      },
      styles: {
        desktop: { padding: '80px 32px' },
      },
    },
    {
      sectionId: 'cta-1',
      tipo: 'CTA',
      nome: 'Chamada para Ação',
      visible: true,
      order: 4,
      background: {
        type: 'color',
        color: '#8B5CF6',
      },
      content: {
        layout: 'contained',
        alignment: 'center',
        elements: [
          {
            id: 'cta-title',
            type: 'heading',
            tag: 'h2',
            content: 'Pronto para começar?',
            styles: {
              desktop: { fontSize: '36px', fontWeight: 'bold', color: '#ffffff', textAlign: 'center' },
            },
          },
          {
            id: 'cta-btn',
            type: 'button',
            content: 'Agendar Agora',
            variant: 'secondary',
            size: 'lg',
            action: { type: 'scroll', href: '#booking' },
            styles: {
              desktop: { marginTop: '24px' },
            },
          },
        ],
      },
      styles: {
        desktop: { padding: '80px 32px', textAlign: 'center' },
      },
    },
    {
      sectionId: 'footer-1',
      tipo: 'FOOTER',
      nome: 'Rodapé',
      visible: true,
      order: 5,
      content: {
        layout: 'contained',
        alignment: 'center',
        elements: [
          {
            id: 'footer-text',
            type: 'paragraph',
            content: '© 2024 {empresa}. Todos os direitos reservados.',
            styles: {
              desktop: { textAlign: 'center', color: '#666' },
            },
          },
          {
            id: 'social-links',
            type: 'social-links',
            props: {
              links: ['instagram', 'facebook', 'whatsapp'],
            },
          },
        ],
      },
      styles: {
        desktop: { padding: '40px 32px', backgroundColor: '#1f2937' },
      },
    },
  ],
};

type ViewMode = 'list' | 'editor';

const HomeLandingPage: React.FC = () => {
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [selectedPage, setSelectedPage] = useState<LandingPageDTO | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const {org} = useAuth();

  // Hooks da API
  const { data: pagesData, isLoading, error } = useLandingPages();
  const createMutation = useCreateLandingPage();
  const deleteMutation = useDeleteLandingPage();
  const duplicateMutation = useDuplicateLandingPage();

  // Se houver erro na API, usa dados de demonstração
  const pages = pagesData?.pages || [demoLandingPage];
  const isDemo = !!error || !pagesData;

  const handleCreatePage = async (nome: string, tipo: LandingPageDTO['tipo']) => {
    try {
      const newPage = await createMutation.mutateAsync({
        nome,
        tipo,
        status: 'DRAFT',
        globalSettings: {
          theme: 'light',
          primaryColor: '#8B5CF6',
        },
        sections: [],
      });
      setSelectedPage(newPage);
      setViewMode('editor');
      setShowCreateModal(false);
    } catch {
      // Em caso de erro, cria uma página local de demonstração
      const localPage: LandingPageDTO = {
        id: `local-${Date.now()}`,
        organizacaoId: Number(org?.id),
        nome,
        slug: nome.toLowerCase().replace(/\s+/g, '-'),
        tipo,
        status: 'DRAFT',
        currentVersion: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        globalSettings: {
          theme: 'light',
          primaryColor: '#8B5CF6',
        },
        sections: [],
      };
      setSelectedPage(localPage);
      setViewMode('editor');
      setShowCreateModal(false);
    }
  };

  const handleEditPage = (page: LandingPageDTO) => {
    setSelectedPage(page);
    setViewMode('editor');
  };

  const handleDeletePage = async (pageId: string) => {
    if (confirm('Tem certeza que deseja excluir esta landing page?')) {
      try {
        await deleteMutation.mutateAsync(pageId);
      } catch {
        console.error('Erro ao excluir página');
      }
    }
  };

  const handleDuplicatePage = async (pageId: string) => {
    try {
      await duplicateMutation.mutateAsync(pageId);
    } catch {
      console.error('Erro ao duplicar página');
    }
  };

  const handleBackToList = () => {
    setViewMode('list');
    setSelectedPage(null);
  };

  // Modo Editor
  if (viewMode === 'editor' && selectedPage) {
    return (
      <div className="h-full flex flex-col">
        {/* Header do Editor */}
        <div className="h-14 flex items-center justify-between px-4 border-b bg-card">
          <div className="flex items-center gap-3">
            <BarbeariaButton
              variant="ghost"
              size="sm"
              onClick={handleBackToList}
            >
              ← Voltar
            </BarbeariaButton>
            <div className="h-6 w-px bg-border" />
            <div>
              <h1 className="font-semibold text-sm">{selectedPage.nome}</h1>
              <p className="text-xs text-muted-foreground">
                {selectedPage.status === 'DRAFT' ? 'Rascunho' : 'Publicado'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {selectedPage.status === 'PUBLISHED' && (
              <BarbeariaButton
                variant="outline"
                size="sm"
                leftIcon={<ExternalLink size={14} />}
                onClick={() => window.open(`/${selectedPage.slug}`, '_blank')}
              >
                Ver Site
              </BarbeariaButton>
            )}
          </div>
        </div>

        {/* Editor */}
        <div className="flex-1 overflow-hidden">
          <LandingPageEditor
            initialData={selectedPage}
            pageId={isDemo ? undefined : selectedPage.id}
          />
        </div>
      </div>
    );
  }

  // Modo Lista
  return (
    <div className="h-full md:px-4 md:py-4 px-2 py-2 overflow-y-auto scrollbar-thin scrollbar-thumb-rounded-full scrollbar-thumb-primary/30 scrollbar-track-transparent">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Landing Pages</h1>
            <p className="text-muted-foreground">
              Crie e gerencie suas páginas de destino
              {isDemo && <span className="text-amber-500 ml-2">(Modo demonstração)</span>}
            </p>
          </div>
          <BarbeariaButton
            variant="primary"
            leftIcon={<Plus size={16} />}
            onClick={() => setShowCreateModal(true)}
          >
            Nova Página
          </BarbeariaButton>
        </div>

        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        )}

        {/* Lista de Páginas */}
        {!isLoading && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {pages.map((page) => (
              <PageCard
                key={page.id}
                page={page}
                onEdit={() => handleEditPage(page)}
                onDelete={() => handleDeletePage(page.id)}
                onDuplicate={() => handleDuplicatePage(page.id)}
              />
            ))}

            {/* Card para criar nova página */}
            <button
              onClick={() => setShowCreateModal(true)}
              className="border-2 border-dashed rounded-xl p-8 flex flex-col items-center justify-center gap-3 text-muted-foreground hover:text-primary hover:border-primary transition-colors min-h-[200px]"
            >
              <Plus className="w-10 h-10" />
              <span className="font-medium">Criar Nova Página</span>
            </button>
          </div>
        )}

        {/* Modal de Criação */}
        {showCreateModal && (
          <CreatePageModal
            onClose={() => setShowCreateModal(false)}
            onCreate={handleCreatePage}
            isLoading={createMutation.isPending}
          />
        )}
      </div>
    </div>
  );
};

// === Card de Página ===
interface PageCardProps {
  page: LandingPageDTO;
  onEdit: () => void;
  onDelete: () => void;
  onDuplicate: () => void;
}

function PageCard({ page, onEdit, onDelete, onDuplicate }: PageCardProps) {
  const statusColors = {
    DRAFT: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
    PUBLISHED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    ARCHIVED: 'bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400',
  };

  const statusLabels = {
    DRAFT: 'Rascunho',
    PUBLISHED: 'Publicado',
    ARCHIVED: 'Arquivado',
  };

  const tipoLabels = {
    HOME: 'Página Inicial',
    PROMOCAO: 'Promoção',
    EVENTO: 'Evento',
    CAMPANHA: 'Campanha',
    CUSTOM: 'Personalizado',
  };

  return (
    <div className="bg-card border rounded-xl overflow-hidden hover:shadow-lg transition-shadow">
      {/* Preview */}
      <div
        className="h-32 bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center cursor-pointer"
        onClick={onEdit}
      >
        <FileText className="w-12 h-12 text-primary/40" />
      </div>

      {/* Info */}
      <div className="p-4 space-y-3">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold truncate">{page.nome}</h3>
            <p className="text-xs text-muted-foreground">{tipoLabels[page.tipo]}</p>
          </div>
          <span className={cn('px-2 py-0.5 rounded-full text-xs font-medium', statusColors[page.status])}>
            {statusLabels[page.status]}
          </span>
        </div>

        <div className="text-xs text-muted-foreground">
          {page.sections.length} seções • Atualizado em{' '}
          {new Date(page.updatedAt).toLocaleDateString('pt-BR')}
        </div>

        {/* Ações */}
        <div className="flex items-center gap-1 pt-2 border-t">
          <BarbeariaButton
            variant="ghost"
            size="xs"
            onClick={onEdit}
            className="flex-1"
          >
            Editar
          </BarbeariaButton>
          <BarbeariaButton
            variant="ghost"
            size="xs"
            iconOnly
            leftIcon={<Copy size={14} />}
            onClick={onDuplicate}
            title="Duplicar"
          />
          <BarbeariaButton
            variant="ghost"
            size="xs"
            iconOnly
            leftIcon={<Trash2 size={14} />}
            onClick={onDelete}
            title="Excluir"
            className="text-red-500 hover:text-red-600"
          />
        </div>
      </div>
    </div>
  );
}

// === Modal de Criação ===
interface CreatePageModalProps {
  onClose: () => void;
  onCreate: (nome: string, tipo: LandingPageDTO['tipo']) => void;
  isLoading: boolean;
}

function CreatePageModal({ onClose, onCreate, isLoading }: CreatePageModalProps) {
  const [nome, setNome] = useState('');
  const [tipo, setTipo] = useState<LandingPageDTO['tipo']>('HOME');

  const tipos: { value: LandingPageDTO['tipo']; label: string; description: string }[] = [
    { value: 'HOME', label: 'Página Inicial', description: 'Página principal do seu site' },
    { value: 'PROMOCAO', label: 'Promoção', description: 'Página para promoções especiais' },
    { value: 'EVENTO', label: 'Evento', description: 'Página para eventos específicos' },
    { value: 'CAMPANHA', label: 'Campanha', description: 'Página para campanhas de marketing' },
    { value: 'CUSTOM', label: 'Personalizado', description: 'Página totalmente customizada' },
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (nome.trim()) {
      onCreate(nome.trim(), tipo);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      <div className="relative bg-white dark:bg-neutral-800 rounded-2xl shadow-2xl w-full max-w-md">
        <form onSubmit={handleSubmit}>
          <div className="px-6 py-4 border-b">
            <h2 className="text-lg font-semibold">Nova Landing Page</h2>
            <p className="text-sm text-muted-foreground">Crie uma nova página de destino</p>
          </div>

          <div className="px-6 py-4 space-y-4">
            <div>
              <label className="text-sm font-medium">Nome da Página</label>
              <input
                type="text"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                placeholder="Ex: Promoção de Verão"
                className="mt-1 w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
                autoFocus
              />
            </div>

            <div>
              <label className="text-sm font-medium">Tipo de Página</label>
              <div className="mt-2 grid grid-cols-1 gap-2">
                {tipos.map((t) => (
                  <button
                    key={t.value}
                    type="button"
                    onClick={() => setTipo(t.value)}
                    className={cn(
                      'flex items-start gap-3 p-3 border rounded-lg text-left transition-all',
                      tipo === t.value
                        ? 'border-primary bg-primary/5'
                        : 'hover:border-gray-300'
                    )}
                  >
                    <div
                      className={cn(
                        'w-4 h-4 mt-0.5 rounded-full border-2 flex-shrink-0',
                        tipo === t.value ? 'border-primary bg-primary' : 'border-gray-300'
                      )}
                    />
                    <div>
                      <p className="font-medium text-sm">{t.label}</p>
                      <p className="text-xs text-muted-foreground">{t.description}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="px-6 py-4 border-t flex justify-end gap-2">
            <BarbeariaButton type="button" variant="ghost" onClick={onClose}>
              Cancelar
            </BarbeariaButton>
            <BarbeariaButton
              type="submit"
              variant="primary"
              disabled={!nome.trim() || isLoading}
              leftIcon={isLoading ? <Loader2 size={14} className="animate-spin" /> : undefined}
            >
              {isLoading ? 'Criando...' : 'Criar Página'}
            </BarbeariaButton>
          </div>
        </form>
      </div>
    </div>
  );
}

export default HomeLandingPage;
