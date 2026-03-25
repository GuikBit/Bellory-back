import { useState } from 'react';
import { X, Type, MousePointer, Palette, LucideIcon } from 'lucide-react';
import { cn } from '../../../../../lib/utils';
import type { LandingPageElement, ResponsiveStyles, ElementAction, ElementIcon } from '../../../../../types/landing-page.types';
import { StyleEditor } from './StyleEditor';

interface ElementEditorProps {
  element: LandingPageElement;
  onChange: (updates: Partial<LandingPageElement>) => void;
  onClose?: () => void;
  className?: string;
}

type EditorTab = 'content' | 'styles' | 'action';

export function ElementEditor({
  element,
  onChange,
  onClose,
  className,
}: ElementEditorProps) {
  const [activeTab, setActiveTab] = useState<EditorTab>('content');

  const tabs: { key: EditorTab; label: string; icon: LucideIcon }[] = [
    { key: 'content', label: 'Conteúdo', icon: Type },
    { key: 'styles', label: 'Estilos', icon: Palette },
    { key: 'action', label: 'Ação', icon: MousePointer },
  ];

  const handleStyleChange = (styles: ResponsiveStyles) => {
    onChange({ styles });
  };

  const handleActionChange = (action: ElementAction | undefined) => {
    onChange({ action });
  };

  const handleIconChange = (icon: ElementIcon | undefined) => {
    onChange({ icon });
  };

  return (
    <div className={cn('flex flex-col h-full', className)}>
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b">
        <div>
          <h3 className="font-semibold text-sm">Editar Elemento</h3>
          <p className="text-xs text-muted-foreground capitalize">{element.type}</p>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            className="p-1 hover:bg-secondary rounded"
          >
            <X size={16} />
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={cn(
              'flex-1 flex items-center justify-center gap-1.5 px-3 py-2 text-sm font-medium transition-all border-b-2',
              activeTab === tab.key
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            )}
          >
            <tab.icon size={14} />
            <span className="hidden sm:inline">{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {activeTab === 'content' && (
          <ContentEditor element={element} onChange={onChange} />
        )}
        {activeTab === 'styles' && (
          <StyleEditor
            styles={element.styles}
            onChange={handleStyleChange}
          />
        )}
        {activeTab === 'action' && (
          <ActionEditor
            action={element.action}
            icon={element.icon}
            onChange={handleActionChange}
            onIconChange={handleIconChange}
          />
        )}
      </div>
    </div>
  );
}

// === Content Editor ===

interface ContentEditorProps {
  element: LandingPageElement;
  onChange: (updates: Partial<LandingPageElement>) => void;
}

function ContentEditor({ element, onChange }: ContentEditorProps) {
  const elementTypeLabels: Record<string, string> = {
    heading: 'Título',
    paragraph: 'Parágrafo',
    text: 'Texto',
    'rich-text': 'Texto Rico',
    button: 'Botão',
    link: 'Link',
    image: 'Imagem',
    video: 'Vídeo',
    icon: 'Ícone',
  };

  return (
    <div className="space-y-4">
      {/* Tipo de elemento (read-only) */}
      <div>
        <label className="text-xs font-medium text-muted-foreground">Tipo</label>
        <p className="mt-1 px-3 py-2 bg-secondary rounded-md text-sm">
          {elementTypeLabels[element.type] || element.type}
        </p>
      </div>

      {/* Campos específicos por tipo */}
      {['heading', 'paragraph', 'text', 'button', 'link'].includes(element.type) && (
        <div>
          <label className="text-xs font-medium text-muted-foreground">
            {element.type === 'heading' || element.type === 'button' ? 'Texto' : 'Conteúdo'}
          </label>
          {element.type === 'paragraph' || element.type === 'rich-text' ? (
            <textarea
              value={element.content || ''}
              onChange={(e) => onChange({ content: e.target.value })}
              className="mt-1 w-full px-3 py-2 border rounded-md text-sm resize-none h-32 focus:outline-none focus:ring-2 focus:ring-primary/50"
              placeholder="Digite o texto..."
            />
          ) : (
            <input
              type="text"
              value={element.content || ''}
              onChange={(e) => onChange({ content: e.target.value })}
              className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              placeholder="Digite o texto..."
            />
          )}
        </div>
      )}

      {/* Tag para headings */}
      {element.type === 'heading' && (
        <div>
          <label className="text-xs font-medium text-muted-foreground">Tag HTML</label>
          <select
            value={element.tag || 'h2'}
            onChange={(e) => onChange({ tag: e.target.value as LandingPageElement['tag'] })}
            className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
          >
            <option value="h1">H1 - Título Principal</option>
            <option value="h2">H2 - Subtítulo</option>
            <option value="h3">H3 - Título de Seção</option>
            <option value="h4">H4 - Subtítulo de Seção</option>
            <option value="h5">H5 - Título Menor</option>
            <option value="h6">H6 - Título Pequeno</option>
          </select>
        </div>
      )}

      {/* Variante para botões */}
      {element.type === 'button' && (
        <>
          <div>
            <label className="text-xs font-medium text-muted-foreground">Variante</label>
            <select
              value={element.variant || 'primary'}
              onChange={(e) => onChange({ variant: e.target.value as LandingPageElement['variant'] })}
              className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            >
              <option value="primary">Primário</option>
              <option value="secondary">Secundário</option>
              <option value="outline">Contorno</option>
              <option value="ghost">Ghost</option>
              <option value="link">Link</option>
            </select>
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">Tamanho</label>
            <select
              value={element.size || 'md'}
              onChange={(e) => onChange({ size: e.target.value as LandingPageElement['size'] })}
              className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            >
              <option value="xs">Extra Pequeno</option>
              <option value="sm">Pequeno</option>
              <option value="md">Médio</option>
              <option value="lg">Grande</option>
              <option value="xl">Extra Grande</option>
            </select>
          </div>
        </>
      )}

      {/* Campos para imagens */}
      {element.type === 'image' && (
        <>
          <div>
            <label className="text-xs font-medium text-muted-foreground">URL da Imagem</label>
            <input
              type="text"
              value={element.url || ''}
              onChange={(e) => onChange({ url: e.target.value })}
              className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              placeholder="https://..."
            />
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">URL Mobile (opcional)</label>
            <input
              type="text"
              value={element.urlMobile || ''}
              onChange={(e) => onChange({ urlMobile: e.target.value })}
              className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              placeholder="https://..."
            />
          </div>

          <div>
            <label className="text-xs font-medium text-muted-foreground">Texto Alternativo (Alt)</label>
            <input
              type="text"
              value={element.alt || ''}
              onChange={(e) => onChange({ alt: e.target.value })}
              className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              placeholder="Descrição da imagem"
            />
          </div>
        </>
      )}

      {/* Campos para vídeo */}
      {element.type === 'video' && (
        <div>
          <label className="text-xs font-medium text-muted-foreground">URL do Vídeo</label>
          <input
            type="text"
            value={element.url || ''}
            onChange={(e) => onChange({ url: e.target.value })}
            className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="https://..."
          />
        </div>
      )}

      {/* Visibilidade */}
      <div className="flex items-center gap-2">
        <input
          type="checkbox"
          id="element-visible"
          checked={element.visible !== false}
          onChange={(e) => onChange({ visible: e.target.checked })}
          className="rounded border-gray-300"
        />
        <label htmlFor="element-visible" className="text-sm">
          Visível
        </label>
      </div>

      {/* Classe CSS customizada */}
      <div>
        <label className="text-xs font-medium text-muted-foreground">Classe CSS (opcional)</label>
        <input
          type="text"
          value={element.className || ''}
          onChange={(e) => onChange({ className: e.target.value })}
          className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
          placeholder="classes-customizadas"
        />
      </div>
    </div>
  );
}

// === Action Editor ===

interface ActionEditorProps {
  action: ElementAction | undefined;
  icon: ElementIcon | undefined;
  onChange: (action: ElementAction | undefined) => void;
  onIconChange: (icon: ElementIcon | undefined) => void;
}

function ActionEditor({ action, icon, onChange, onIconChange }: ActionEditorProps) {
  const actionTypes: { value: ElementAction['type']; label: string }[] = [
    { value: 'link', label: 'Link Externo' },
    { value: 'scroll', label: 'Scroll para Seção' },
    { value: 'whatsapp', label: 'WhatsApp' },
    { value: 'phone', label: 'Telefone' },
    { value: 'email', label: 'Email' },
    { value: 'modal', label: 'Abrir Modal' },
  ];

  const handleTypeChange = (type: string) => {
    if (!type) {
      onChange(undefined);
      return;
    }
    onChange({
      type: type as ElementAction['type'],
      href: action?.href || '',
      target: action?.target,
    });
  };

  return (
    <div className="space-y-4">
      {/* Ícone */}
      <div className="p-3 border rounded-lg space-y-3">
        <h4 className="text-sm font-medium">Ícone</h4>

        <div>
          <label className="text-xs text-muted-foreground">Nome do Ícone (Lucide)</label>
          <input
            type="text"
            value={icon?.name || ''}
            onChange={(e) =>
              onIconChange(e.target.value ? { ...icon, name: e.target.value } : undefined)
            }
            className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="calendar, phone, mail..."
          />
        </div>

        {icon?.name && (
          <>
            <div>
              <label className="text-xs text-muted-foreground">Posição</label>
              <select
                value={icon.position || 'left'}
                onChange={(e) =>
                  onIconChange({ ...icon, position: e.target.value as 'left' | 'right' })
                }
                className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
              >
                <option value="left">Esquerda</option>
                <option value="right">Direita</option>
              </select>
            </div>

            <div>
              <label className="text-xs text-muted-foreground">Tamanho (px)</label>
              <input
                type="number"
                value={icon.size || 16}
                onChange={(e) =>
                  onIconChange({ ...icon, size: parseInt(e.target.value) || 16 })
                }
                className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
                min={8}
                max={64}
              />
            </div>
          </>
        )}
      </div>

      {/* Ação */}
      <div className="p-3 border rounded-lg space-y-3">
        <h4 className="text-sm font-medium">Ação ao Clicar</h4>

        <div>
          <label className="text-xs text-muted-foreground">Tipo de Ação</label>
          <select
            value={action?.type || ''}
            onChange={(e) => handleTypeChange(e.target.value)}
            className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
          >
            <option value="">Nenhuma ação</option>
            {actionTypes.map((type) => (
              <option key={type.value} value={type.value}>
                {type.label}
              </option>
            ))}
          </select>
        </div>

        {action?.type && (
          <>
            <div>
              <label className="text-xs text-muted-foreground">
                {action.type === 'scroll' && 'ID da Seção (ex: #contato)'}
                {action.type === 'link' && 'URL do Link'}
                {action.type === 'whatsapp' && 'Número do WhatsApp'}
                {action.type === 'phone' && 'Número de Telefone'}
                {action.type === 'email' && 'Endereço de Email'}
                {action.type === 'modal' && 'ID do Modal'}
              </label>
              <input
                type="text"
                value={action.href || ''}
                onChange={(e) => onChange({ ...action, href: e.target.value })}
                className="mt-1 w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
                placeholder={
                  action.type === 'scroll'
                    ? '#contato'
                    : action.type === 'link'
                    ? 'https://...'
                    : action.type === 'whatsapp'
                    ? '5511999999999'
                    : action.type === 'phone'
                    ? '11999999999'
                    : action.type === 'email'
                    ? 'contato@empresa.com'
                    : 'modal-id'
                }
              />
            </div>

            {action.type === 'link' && (
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="action-target"
                  checked={action.target === '_blank'}
                  onChange={(e) =>
                    onChange({ ...action, target: e.target.checked ? '_blank' : '_self' })
                  }
                  className="rounded border-gray-300"
                />
                <label htmlFor="action-target" className="text-sm">
                  Abrir em nova aba
                </label>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
