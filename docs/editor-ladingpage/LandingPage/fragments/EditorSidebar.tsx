import { useState } from 'react';
import { ChevronDown, ChevronRight, X, Sparkles, Sidebar } from 'lucide-react';
import { PageSection, SubSection } from '../../../../../utils/interfaces';
import { cn } from '../../../../../lib/utils';
import { SectionIcon } from './SectionIcon';
import { useTheme } from '../../../../../global/Theme-context';
import { BarbeariaButton } from '../../../../ui';


interface EditorSidebarProps {
  menuPaginas: PageSection[];
  selectedPage: string | null;
  selectedSection: string | null;
  onSelectPage: (pageId: string) => void;
  onSelectSection: (sectionId: string, pageId: string) => void;
  isMobile?: boolean;
  onClose?: () => void;
}

export const EditorSidebar = ({
  menuPaginas,
  selectedPage,
  selectedSection,
  onSelectPage,
  onSelectSection,
  isMobile = false,
  onClose,
}: EditorSidebarProps) => {
    const {currentTheme: theme} = useTheme();
    const [expandedPages, setExpandedPages] = useState<string[]>(['default', 'home']);

    const toggleExpand = (pageId: string) => {
        setExpandedPages((prev) =>
        prev.includes(pageId)
            ? prev.filter((id) => id !== pageId)
            : [...prev, pageId]
        );
    };

    const handlePageClick = (page: PageSection) => {
        onSelectPage(page.id);
        if (page.subSessoes && page.subSessoes.length > 0) {
        toggleExpand(page.id);
        }
    };

    const handleSectionClick = (section: SubSection, pageId: string) => {
        onSelectSection(section.id, pageId);
        section.command?.();
        if (isMobile && onClose) {
        onClose();
        }
    };

    return (
        <div className="h-full flex flex-col ">
            <div className="h-[50px] flex items-center justify-between px-4 py-4 border-b dark:border-neutral-700 border-gray-200">
                <div className="flex items-center justify-between w-full">
                    <div className='flex items-center gap-2'>
                        <div className="w-8 h-8 rounded-lg gradient-primary flex items-center justify-center">
                            <Sparkles className="w-4 h-4 text-primary" />
                        </div>
                        <div>
                            <h2 className="font-semibold text-foreground text-sm ">Editor</h2>
                            <p className="text-[10px] text-muted-foreground">Landing Page</p>
                        </div>
                    </div>
                    <BarbeariaButton variant="ghost" size="sm" iconOnly leftIcon={<Sidebar size={16} />} onClick={()=>{}} rounded="full" />
                </div>
                {isMobile && onClose && (
                    <button
                        onClick={onClose}
                        className="p-2 rounded-lg hover:bg-secondary transition-colors"
                        aria-label="Fechar menu"
                    >
                        <X className="w-5 h-5 text-muted-foreground" />
                    </button>
                )}
            </div>

            <nav className="flex-1 overflow-y-auto  scrollbar-thin scrollbar-thumb-rounded-full scrollbar-thumb-primary/30 scrollbar-track-transparent py-3 px-2">
                <div className="space-y-1">
                    {menuPaginas.map((page) => {
                        const isExpanded = expandedPages.includes(page.id);
                        const hasSubSections = page.subSessoes && page.subSessoes.length > 0;
                        const isSelected = selectedPage === page.id;

                        return (
                        <div key={page.id} className="space-y-0.5">
                            
                            <button
                                onClick={() => handlePageClick(page)}
                                className={cn(
                                    'w-full flex items-center justify-between group cursor-pointer dark:hover:bg-neutral-700 hover:bg-neutral-100 rounded-lg px-2 py-2.5 text-sm font-medium transition-all duration-200',
                                    isSelected && !hasSubSections && 'text-primary'
                                )}
                                data-pr-tooltip={page.description}
                                data-pr-position="right"
                            >
                                <div className="flex items-center gap-2.5">
                                    <SectionIcon
                                        sectionId={page.id}
                                        className={cn(
                                            'w-4 h-4 transition-colors',
                                        )}
                                        style={{
                                            color: isSelected ? theme.colors.primary : theme.colors.textSecondary,
                                        }}
                                    />
                                    <span>
                                        {page.label}
                                    </span>
                                    
                                </div>
                                {page.isNew && (
                                    <span className="text-xs">Novo</span>
                                )}
                                {hasSubSections && (
                                    <span className="text-muted-foreground">
                                        {isExpanded ? (
                                            <ChevronDown className="w-4 h-4" style={{color: theme.colors.textSecondary}} />
                                        ) : (
                                            <ChevronRight className="w-4 h-4" style={{color: theme.colors.textSecondary}} />
                                        )}
                                    </span>
                                )}
                            </button>

                            {/* Sub Sections */}
                            {hasSubSections && isExpanded && (
                                <div className={cn('ml-4 pl-3 border-l-2 space-y-0.5 animate-fade-in ','dark:border-neutral-600 border-neutral-300')}>
                                    {page.subSessoes!.map((section: any) => {
                                        const isSectionSelected =
                                        selectedSection === section.id && selectedPage === page.id;

                                        return (
                                            <button
                                                key={`${page.id}-${section.id}`}
                                                onClick={() => handleSectionClick(section, page.id)}
                                                className={cn(
                                                    'w-full flex items-center justify-between gap-2.5 dark:hover:bg-neutral-100/10 cursor-pointer group hover:bg-neutral-100 rounded-lg px-3 py-2 text-sm font-medium transition-all duration-200',
                                                    isSectionSelected && 'dark:bg-neutral-100/10 bg-neutral-100 text-primary-foreground'
                                                )}
                                                data-pr-tooltip={section.description}
                                                data-pr-position="right"
                                            >
                                                <div className='w-full flex items-center gap-2.5'>
                                                    <SectionIcon
                                                        sectionId={section.id}
                                                        className={cn(
                                                        'w-3.5 h-3.5 transition-colors',
                                                        )}
                                                        style={{
                                                            color: isSectionSelected ? theme.colors.primary : theme.colors.textSecondary,
                                                        }}
                                                    
                                                    />
                                                    <span className={cn('text-sm', isSectionSelected?'text-primary':'text-muted-foreground group-hover:text-foreground text-neutral-700 dark:text-neutral-300')}>{section.label}</span>
                                                </div>
                                                {section.isNew && (
                                                    <div className="text-[9px] text-end bg-primary dark:text-black text-white px-1 rounded">Novo</div>
                                                )}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                        );
                    })}
                </div>
            </nav>

        </div>
    );
};
