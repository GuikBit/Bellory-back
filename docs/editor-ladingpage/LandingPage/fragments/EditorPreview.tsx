import { Monitor, Tablet, Smartphone, Eye, Settings2, Undo2, Redo2, Save, Loader2, Check, ZoomIn, ZoomOut, Maximize } from 'lucide-react';
import { useState, useCallback } from 'react';
import { cn } from '../../../../../lib/utils';
import { BarbeariaButton } from '../../../../ui';
import { useTheme } from '../../../../../global/Theme-context';
import type { LandingPageDTO, LandingPageSection, LandingPageElement, DeviceType } from '../../../../../types/landing-page.types';
import { DevicePreview } from './DevicePreview';
import { SectionPreviewRenderer } from '../preview/SectionPreviewRenderer';
import { AddElementPanel } from './AddElementPanel';


interface EditorPreviewProps {
  selectedPage: string | null;
  selectedSection: string | null;
  selectedElementId?: string | null;
  // Novas props para integração com o store
  landingPage?: LandingPageDTO | null;
  devicePreview?: DeviceType;
  onDeviceChange?: (device: DeviceType) => void;
  onSectionSelect?: (sectionId: string) => void;
  onElementSelect?: (elementId: string) => void;
  onSectionUpdate?: (sectionId: string, updates: Partial<LandingPageSection>) => void;
  onSectionDuplicate?: (sectionId: string) => void;
  onSectionDelete?: (sectionId: string) => void;
  onSectionToggleVisibility?: (sectionId: string) => void;
  onAddElement?: (sectionId: string, element: Partial<LandingPageElement>) => void;
  onUndo?: () => void;
  onRedo?: () => void;
  canUndo?: boolean;
  canRedo?: boolean;
  onSave?: () => void;
  onPreview?: () => void;
  onSettings?: () => void;
  isDirty?: boolean;
  isSaving?: boolean;
  lastSavedText?: string;
  variables?: Record<string, string>;
}

export const EditorPreview = ({
  selectedPage,
  selectedSection,
  selectedElementId,
  landingPage,
  devicePreview: externalDevice,
  onDeviceChange,
  onSectionSelect,
  onElementSelect,
  onSectionDuplicate,
  onSectionDelete,
  onSectionToggleVisibility,
  onAddElement,
  onUndo,
  onRedo,
  canUndo = false,
  canRedo = false,
  onSave,
  onPreview,
  onSettings,
  isDirty = false,
  isSaving = false,
  lastSavedText,
  variables = {},
}: EditorPreviewProps) => {
    const [internalDevice, setInternalDevice] = useState<DeviceType>('desktop');
    const [zoom, setZoom] = useState(100);
    const [addElementSectionId, setAddElementSectionId] = useState<string | null>(null);
    const { currentTheme: theme} = useTheme();

    // Usa device externo se fornecido, senão usa interno
    const device = externalDevice || internalDevice;
    const setDevice = onDeviceChange || setInternalDevice;

    const deviceOptions = [
        { icon: Monitor, value: 'desktop' as DeviceType, tooltip: 'Desktop' },
        { icon: Tablet, value: 'tablet' as DeviceType, tooltip: 'Tablet' },
        { icon: Smartphone, value: 'mobile' as DeviceType, tooltip: 'Celular' },
    ];

    const handleZoomIn = useCallback(() => {
      setZoom((prev) => Math.min(prev + 10, 150));
    }, []);

    const handleZoomOut = useCallback(() => {
      setZoom((prev) => Math.max(prev - 10, 50));
    }, []);

    const handleZoomReset = useCallback(() => {
      setZoom(100);
    }, []);

    // Obtém as seções ordenadas
    const sections = landingPage?.sections
      ? [...landingPage.sections].sort((a, b) => a.order - b.order)
      : [];

  return (
    <div className="h-full flex flex-col">
      {/* Toolbar */}
      <div className="flex flex-wrap h-[50px] items-center justify-between gap-3 px-4 py-0 bg-card border-b dark:border-neutral-700 border-gray-200">
        <div className="flex items-center gap-2">
          {/* Undo/Redo */}
          <BarbeariaButton
            variant="ghost"
            size="sm"
            iconOnly
            leftIcon={<Undo2 size={16} />}
            onClick={onUndo}
            rounded="full"
            disabled={!canUndo}
            title="Desfazer (Ctrl+Z)"
          />
          <BarbeariaButton
            variant="ghost"
            size="sm"
            iconOnly
            leftIcon={<Redo2 size={16} />}
            onClick={onRedo}
            rounded="full"
            disabled={!canRedo}
            title="Refazer (Ctrl+Shift+Z)"
          />

          <div className="w-px h-6 bg-border mx-1" />

          {/* Device Selector */}
          <div className="hidden sm:flex items-center gap-1 bg-secondary rounded-full p-0.5 " style={{ backgroundColor: theme.colors.primary}}>
            {deviceOptions.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setDevice(opt.value)}
                className={cn(
                  'p-1.5 rounded-full transition-all duration-200 ',
                  device === opt.value
                    ? 'shadow-sm bg-white dark:bg-neutral-800 text-primary cursor-pointer'
                    : 'text-muted-foreground text-white cursor-pointer'
                )}
                title={opt.tooltip}
              >
                <opt.icon size={16} />
              </button>
            ))}
          </div>

          {/* Zoom Controls */}
          <div className="hidden md:flex items-center gap-1 ml-2">
            <button
              onClick={handleZoomOut}
              className="p-1.5 hover:bg-secondary rounded transition-colors"
              title="Diminuir zoom"
              disabled={zoom <= 50}
            >
              <ZoomOut size={14} className="text-muted-foreground" />
            </button>
            <span className="text-xs text-muted-foreground w-10 text-center">
              {zoom}%
            </span>
            <button
              onClick={handleZoomIn}
              className="p-1.5 hover:bg-secondary rounded transition-colors"
              title="Aumentar zoom"
              disabled={zoom >= 150}
            >
              <ZoomIn size={14} className="text-muted-foreground" />
            </button>
            <button
              onClick={handleZoomReset}
              className="p-1.5 hover:bg-secondary rounded transition-colors"
              title="Reset zoom"
            >
              <Maximize size={14} className="text-muted-foreground" />
            </button>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Save Status */}
          {isDirty && !isSaving && (
            <span className="text-xs text-amber-500 hidden sm:block">Alterações não salvas</span>
          )}
          {isSaving && (
            <span className="text-xs text-muted-foreground flex items-center gap-1">
              <Loader2 size={12} className="animate-spin" />
              Salvando...
            </span>
          )}
          {!isDirty && !isSaving && lastSavedText && (
            <span className="text-xs text-green-500 flex items-center gap-1 hidden sm:flex">
              <Check size={12} />
              {lastSavedText}
            </span>
          )}

          {/* Actions */}
          {onSave && (
            <BarbeariaButton
              variant="ghost"
              size="xs"
              leftIcon={isSaving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
              onClick={onSave}
              rounded="full"
              disabled={isSaving || !isDirty}
            >
              Salvar
            </BarbeariaButton>
          )}
          <BarbeariaButton
            variant="ghost"
            size="xs"
            leftIcon={<Eye size={16} />}
            onClick={onPreview}
            rounded="full"
          >
            Visualizar
          </BarbeariaButton>
          <BarbeariaButton
            variant="ghost"
            size="sm"
            iconOnly
            leftIcon={<Settings2 size={16} />}
            onClick={onSettings}
            rounded="full"
          />
        </div>
      </div>

      {/* Preview Area */}
      <div className="flex-1 overflow-auto">
        <DevicePreview device={device} zoom={zoom}>
          {landingPage && sections.length > 0 ? (
            // Renderiza as seções da landing page
            <div className="min-h-full">
              {sections.map((section) => (
                <SectionPreviewRenderer
                  key={section.sectionId}
                  section={section}
                  device={device}
                  variables={variables}
                  isSelected={selectedSection === section.sectionId}
                  isEditing={true}
                  selectedElementId={selectedElementId}
                  onClick={() => onSectionSelect?.(section.sectionId)}
                  onElementClick={(elementId) => onElementSelect?.(elementId)}
                  onToggleVisibility={() => onSectionToggleVisibility?.(section.sectionId)}
                  onDuplicate={() => onSectionDuplicate?.(section.sectionId)}
                  onDelete={() => onSectionDelete?.(section.sectionId)}
                  onAddElement={() => setAddElementSectionId(section.sectionId)}
                  showControls={true}
                />
              ))}
            </div>
          ) : selectedSection ? (
            // Fallback para compatibilidade - mostra placeholder quando seção está selecionada
            <div className="w-full min-h-full bg-gradient-to-br from-primary/5 to-primary/10">
              <div className="h-16 bg-white/80 dark:bg-neutral-800/80 backdrop-blur-sm border-b flex items-center justify-center">
                <span className="text-sm text-muted-foreground">Preview: {selectedSection}</span>
              </div>
              <div className="flex items-center justify-center h-[calc(100%-4rem)]">
                <div className="text-center space-y-3 p-8">
                  <div className="w-12 h-12 mx-auto rounded-xl bg-primary/10 flex items-center justify-center">
                    <Monitor className="w-6 h-6 text-primary" />
                  </div>
                  <p className="text-sm text-muted-foreground">
                    Passe dados iniciais para visualizar o preview completo
                  </p>
                </div>
              </div>
            </div>
          ) : (
            // Estado vazio
            <div className="h-full flex items-center justify-center">
              <div className="text-center space-y-4 p-8">
                <div className="w-16 h-16 mx-auto rounded-2xl bg-secondary flex items-center justify-center">
                  <Monitor className="w-8 h-8 text-muted-foreground" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-foreground">
                    Selecione uma seção
                  </h3>
                  <p className="text-sm text-muted-foreground mt-1 max-w-sm mx-auto">
                    Clique em uma seção no menu lateral para começar a editar sua landing page.
                  </p>
                </div>
              </div>
            </div>
          )}
        </DevicePreview>
      </div>

      {/* Status Bar */}
      <div className="px-4 py-2 bg-card border-t dark:border-neutral-700 border-gray-200 flex items-center justify-between text-xs text-muted-foreground">
        <span>
          {landingPage ? (
            <>
              <span className="font-medium">{landingPage.nome}</span>
              {selectedSection && (
                <span className="text-muted-foreground"> → {sections.find(s => s.sectionId === selectedSection)?.nome || selectedSection}</span>
              )}
            </>
          ) : selectedPage ? (
            <>
              📄 {selectedPage}
              {selectedSection && ` → ${selectedSection}`}
            </>
          ) : (
            'Nenhuma página selecionada'
          )}
        </span>
        <div className="flex items-center gap-3">
          <span className="hidden sm:block capitalize">{device}</span>
          {lastSavedText && <span className="hidden md:block">{lastSavedText}</span>}
        </div>
      </div>

      {/* Add Element Panel */}
      {addElementSectionId && (
        <AddElementPanel
          onAddElement={(element) => {
            onAddElement?.(addElementSectionId, element);
            setAddElementSectionId(null);
          }}
          onClose={() => setAddElementSectionId(null)}
        />
      )}
    </div>
  );
};
