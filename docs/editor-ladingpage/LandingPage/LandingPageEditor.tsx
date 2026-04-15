import { useState, useEffect, useCallback } from 'react';
import { Menu, PanelRightOpen, PanelRightClose } from 'lucide-react';
import { PageSection } from '../../../../utils/interfaces';
// import { defaultMenuPaginas } from './fragments/data';
import { cn } from '../../../../lib/utils';
// import { EditorSidebar } from './fragments/EditorSidebar';
import { EditorPreview } from './fragments/EditorPreview';
import { SectionListSidebar } from './fragments/SectionListSidebar';
import { ElementEditor } from './fragments/ElementEditor';
import { SectionEditorRouter } from './editors/SectionEditorRouter';
import { EditorProvider, useEditor, useEditorState, useSelectedSection, useSelectedElement } from '../../../../stores/editorStore';
import { useAutoSave, useLastSavedText } from '../../../../hooks/useAutoSave';
import { useUpdateLandingPage } from '../../../../hooks/useLandingPage';
import type { LandingPageDTO, SectionType } from '../../../../types/landing-page.types';
import { useAuth } from '../../../../global/AuthContext';
import { EditorSiteProvider } from './preview/EditorSiteProvider';


// === Props ===
interface LandingPageEditorProps {
  menuPaginas?: PageSection[];
  className?: string;
  pageId?: string; // ID da landing page para carregar da API
  initialData?: LandingPageDTO; // Dados iniciais (para modo offline/demo)
}

// === Componente Principal do Editor ===
export const LandingPageEditor = ({
  // menuPaginas = defaultMenuPaginas,
  pageId,
  initialData,
}: LandingPageEditorProps) => {
  // Determina o modo do editor
  const isApiMode = !!pageId || !!initialData;

  if (isApiMode) {
    return (
      <EditorProvider>
        <FullEditor pageId={pageId} initialData={initialData} />
      </EditorProvider>
    );
  }

  // Modo legado (compatibilidade com menu de páginas)
  // return <LegacyEditor menuPaginas={menuPaginas} />;
};

// === Editor Completo (com API/Store) ===
interface FullEditorProps {
  pageId?: string;
  initialData?: LandingPageDTO;
}

function FullEditor({ pageId, initialData }: FullEditorProps) {
  const editor = useEditor();
  const {
    landingPage,
    selectedSectionId,
    devicePreview,
    isDirty,
    isSaving,
    propertiesPanelOpen,
    isLoading,
  } = useEditorState();

  const selectedSection = useSelectedSection();
  const selectedElement = useSelectedElement();

  const [isMobile, setIsMobile] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const updateMutation = useUpdateLandingPage();

  // Carrega dados iniciais
  useEffect(() => {
    if (initialData) {
      editor.setLandingPage(initialData);
    }
  }, [initialData]);

  // Auto-save
  const { lastSavedAt } = useAutoSave(landingPage, isDirty, {
    debounceMs: 3000,
    enabled: !!pageId && isDirty,
    onSave: async (data) => {
      if (pageId) {
        await updateMutation.mutateAsync({ id: pageId, data });
        editor.setIsDirty(false);
      }
    },
  });

  const lastSavedText = useLastSavedText(lastSavedAt);

  // Check for mobile viewport
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ctrl+Z - Undo
      if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
        e.preventDefault();
        editor.undo();
      }
      // Ctrl+Shift+Z or Ctrl+Y - Redo
      if ((e.ctrlKey || e.metaKey) && (e.key === 'Z' || e.key === 'y')) {
        e.preventDefault();
        editor.redo();
      }
      // Ctrl+S - Save
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        editor.save();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [editor]);

  // Variáveis dinâmicas da organização
  const { org } = useAuth();
  const variables: Record<string, string> = {
    '{empresa}': org?.nomeFantasia || 'Sua Empresa',
    '{telefone}': org?.telefone1 || '(00) 0000-0000',
    '{whatsapp}': org?.whatsapp || '00000000000',
    '{email}': org?.emailPrincipal || 'contato@empresa.com',
    '{endereco}': org?.enderecoPrincipal
      ? `${org.enderecoPrincipal.logradouro}, ${org.enderecoPrincipal.numero} - ${org.enderecoPrincipal.bairro}`
      : 'Endereço não definido',
    '{cidade}': org?.enderecoPrincipal?.cidade || 'Cidade',
    '{descricao_empresa}': org?.razaoSocial || 'Descrição da empresa',
  };

  const handleSave = useCallback(async () => {
    await editor.save();
  }, [editor]);

  const handleAddSection = useCallback((tipo: SectionType) => {
    editor.addSection(tipo);
  }, [editor]);

  if (isLoading) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-center space-y-4">
          <div className="w-16 h-16 mx-auto rounded-full border-4 border-primary/20 border-t-primary animate-spin" />
          <p className="text-muted-foreground">Carregando editor...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full md:px-4 md:py-4 px-2 py-2 overflow-y-auto scrollbar-thin scrollbar-thumb-rounded-full scrollbar-thumb-primary/30 scrollbar-track-transparent space-y-4">
      <div
        className="overflow-auto space-y-6 border rounded-[18px] bg-white dark:bg-neutral-800 border-gray-200 dark:border-neutral-700/50"
        style={{
          height: 'calc(100vh - 98px)',
        }}
      >
        {/* Mobile Header */}
        <header className="md:hidden flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-neutral-700">
          <button
            onClick={() => setSidebarOpen(true)}
            className="p-2 hover:bg-secondary rounded-lg"
          >
            <Menu className="w-5 h-5" />
          </button>
          <h1 className="font-semibold text-foreground">Editor de Landing Page</h1>
          <button
            onClick={editor.togglePropertiesPanel}
            className="p-2 hover:bg-secondary rounded-lg"
          >
            {propertiesPanelOpen ? (
              <PanelRightClose className="w-5 h-5" />
            ) : (
              <PanelRightOpen className="w-5 h-5" />
            )}
          </button>
        </header>

        <div className="flex-1 h-full flex overflow-hidden">
          {/* Sidebar Desktop - Lista de Seções */}
          <aside className="hidden md:block w-64 border-r dark:border-neutral-700 border-gray-200 flex-shrink-0">
            <SectionListSidebar
              sections={landingPage?.sections || []}
              selectedSectionId={selectedSectionId}
              onSelectSection={(id) => editor.selectSection(id)}
              onAddSection={handleAddSection}
              onDuplicateSection={(id) => editor.duplicateSection(id)}
              onDeleteSection={(id) => editor.removeSection(id)}
              onToggleVisibility={(id) => editor.toggleSectionVisibility(id)}
            />
          </aside>

          {/* Mobile Sidebar Overlay */}
          {isMobile && (
            <>
              <div
                className={cn(
                  'fixed inset-0 bg-foreground/50 z-40 transition-opacity duration-300',
                  sidebarOpen
                    ? 'opacity-100 pointer-events-auto'
                    : 'opacity-0 pointer-events-none'
                )}
                onClick={() => setSidebarOpen(false)}
                aria-hidden="true"
              />

              <aside
                className={cn(
                  'fixed inset-y-0 left-0 w-72 max-w-[85vw] bg-white dark:bg-neutral-800 z-9000 shadow-elevated transition-transform duration-300 ease-out',
                  sidebarOpen ? 'translate-x-0' : '-translate-x-full'
                )}
              >
                <SectionListSidebar
                  sections={landingPage?.sections || []}
                  selectedSectionId={selectedSectionId}
                  onSelectSection={(id) => editor.selectSection(id)}
                  onAddSection={handleAddSection}
                  onDuplicateSection={(id) => editor.duplicateSection(id)}
                  onDeleteSection={(id) => editor.removeSection(id)}
                  onToggleVisibility={(id) => editor.toggleSectionVisibility(id)}
                  isMobile
                  onClose={() => setSidebarOpen(false)}
                />
              </aside>
            </>
          )}

          {/* Preview Central */}
          <main className="flex-1 overflow-hidden">
            <EditorSiteProvider>
            <EditorPreview
              selectedPage={null}
              selectedSection={selectedSectionId}
              selectedElementId={selectedElement?.element.id || null}
              landingPage={landingPage}
              devicePreview={devicePreview}
              onDeviceChange={(device) => editor.setDevicePreview(device)}
              onSectionSelect={(id) => editor.selectSection(id)}
              onElementSelect={(id) => editor.selectElement(id)}
              onSectionDuplicate={(id) => editor.duplicateSection(id)}
              onSectionDelete={(id) => editor.removeSection(id)}
              onSectionToggleVisibility={(id) => editor.toggleSectionVisibility(id)}
              onAddElement={(sectionId, element) => editor.addElement(sectionId, element)}
              onUndo={editor.undo}
              onRedo={editor.redo}
              canUndo={editor.canUndo()}
              canRedo={editor.canRedo()}
              onSave={handleSave}
              isDirty={isDirty}
              isSaving={isSaving}
              lastSavedText={lastSavedText}
              variables={variables}
            />
            </EditorSiteProvider>
          </main>

          {/* Properties Panel */}
          {propertiesPanelOpen && (selectedSection || selectedElement) && (
            <aside className="hidden lg:block w-80 border-l dark:border-neutral-700 border-gray-200 flex-shrink-0 overflow-y-auto">
              {selectedElement ? (
                <ElementEditor
                  element={selectedElement.element}
                  onChange={(updates) =>
                    editor.updateElement(
                      selectedElement.section.sectionId,
                      selectedElement.element.id,
                      updates
                    )
                  }
                  onClose={() => editor.selectElement(null)}
                />
              ) : selectedSection ? (
                <SectionEditorRouter
                  section={selectedSection}
                  editor={editor}
                />
              ) : null}
            </aside>
          )}
        </div>

        {/* Mobile Floating Button */}
        {isMobile && !sidebarOpen && (
          <button
            onClick={() => setSidebarOpen(true)}
            className="editor-floating-button bottom-6 left-6"
            aria-label="Abrir seções"
          >
            <Menu className="w-5 h-5" />
          </button>
        )}
      </div>
    </div>
  );
}

// === Editor Legado (Compatibilidade) ===
// interface LegacyEditorProps {
//   menuPaginas: PageSection[];
// }

// function LegacyEditor({ menuPaginas }: LegacyEditorProps) {
//   const [state, setState] = useState<LegacyEditorState>({
//     selectedPage: null,
//     selectedSection: null,
//     isSidebarOpen: false,
//   });

//   const [isMobile, setIsMobile] = useState(false);

//   // Check for mobile viewport
//   useEffect(() => {
//     const checkMobile = () => {
//       setIsMobile(window.innerWidth < 768);
//     };

//     checkMobile();
//     window.addEventListener('resize', checkMobile);
//     return () => window.removeEventListener('resize', checkMobile);
//   }, []);

//   const handleSelectPage = (pageId: string) => {
//     setState((prev) => ({
//       ...prev,
//       selectedPage: pageId,
//       selectedSection: null,
//     }));
//   };

//   const handleSelectSection = (sectionId: string, pageId: string) => {
//     setState((prev) => ({
//       ...prev,
//       selectedPage: pageId,
//       selectedSection: sectionId,
//     }));
//   };

//   const toggleSidebar = () => {
//     setState((prev) => ({
//       ...prev,
//       isSidebarOpen: !prev.isSidebarOpen,
//     }));
//   };

//   const closeSidebar = () => {
//     setState((prev) => ({
//       ...prev,
//       isSidebarOpen: false,
//     }));
//   };

//   return (
//     <div className="h-full md:px-4 md:py-4 px-2 py-2 overflow-y-auto scrollbar-thin scrollbar-thumb-rounded-full scrollbar-thumb-primary/30 scrollbar-track-transparent space-y-4">
//       <div
//         className="overflow-auto space-y-6 border rounded-[18px] bg-white dark:bg-neutral-800 border-gray-200 dark:border-neutral-700/50"
//         style={{
//           height: 'calc(100vh - 98px)',
//         }}
//       >
//         {/* Mobile Header */}
//         <header className="md:hidden flex items-center justify-center px-4 py-3 border-b border-gray-200 dark:border-neutral-700">
//           <h1 className="font-semibold text-foreground">Editor de Landing Page</h1>
//         </header>

//         <div className="flex-1 h-full flex overflow-hidden">
//           {/* Desktop Sidebar */}
//           <aside className="hidden md:block w-65 border-r dark:border-neutral-700 border-gray-200 flex-shrink-0">
//             <EditorSidebar
//               menuPaginas={menuPaginas}
//               selectedPage={state.selectedPage}
//               selectedSection={state.selectedSection}
//               onSelectPage={handleSelectPage}
//               onSelectSection={handleSelectSection}
//             />
//           </aside>

//           {/* Mobile Sidebar */}
//           {isMobile && (
//             <>
//               <div
//                 className={cn(
//                   'fixed inset-0 bg-foreground/50 z-40 transition-opacity duration-300',
//                   state.isSidebarOpen
//                     ? 'opacity-100 pointer-events-auto'
//                     : 'opacity-0 pointer-events-none'
//                 )}
//                 onClick={closeSidebar}
//                 aria-hidden="true"
//               />

//               <aside
//                 className={cn(
//                   'fixed inset-y-0 left-0 w-72 max-w-[85vw] bg-white dark:bg-neutral-800 z-9000 shadow-elevated transition-transform duration-300 ease-out',
//                   state.isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
//                 )}
//               >
//                 <EditorSidebar
//                   menuPaginas={menuPaginas}
//                   selectedPage={state.selectedPage}
//                   selectedSection={state.selectedSection}
//                   onSelectPage={handleSelectPage}
//                   onSelectSection={handleSelectSection}
//                   isMobile
//                   onClose={closeSidebar}
//                 />
//               </aside>
//             </>
//           )}

//           <main className="flex-1 overflow-hidden">
//             <SiteProvider>
//               {/* <TemplateContent /> */}

//               <EditorPreview
//                 selectedPage={state.selectedPage}
//                 selectedSection={state.selectedSection}
//               />
//             </SiteProvider>
            
//             <EditorPreview
//               selectedPage={state.selectedPage}
//               selectedSection={state.selectedSection}

//             />
//           </main>
//         </div>

//         {isMobile && !state.isSidebarOpen && (
//           <button
//             onClick={toggleSidebar}
//             className="editor-floating-button bottom-6 left-6"
//             aria-label="Abrir seções"
//           >
//             <Menu className="w-5 h-5" />
//           </button>
//         )}
//       </div>
//     </div>
//   );
// }

export default LandingPageEditor;
