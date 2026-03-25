import { useState } from 'react';
import { Monitor, Tablet, Smartphone, ChevronDown } from 'lucide-react';
import { cn } from '../../../../../lib/utils';
import type { ResponsiveStyles, DeviceType } from '../../../../../types/landing-page.types';

interface StyleEditorProps {
  styles: ResponsiveStyles | undefined;
  onChange: (styles: ResponsiveStyles) => void;
  showDeviceTabs?: boolean;
  className?: string;
}

type StyleCategory = 'typography' | 'spacing' | 'layout' | 'background' | 'border' | 'effects';

const deviceIcons = {
  desktop: Monitor,
  tablet: Tablet,
  mobile: Smartphone,
};

const styleCategories: { key: StyleCategory; label: string }[] = [
  { key: 'typography', label: 'Tipografia' },
  { key: 'spacing', label: 'Espaçamento' },
  { key: 'layout', label: 'Layout' },
  { key: 'background', label: 'Fundo' },
  { key: 'border', label: 'Borda' },
  { key: 'effects', label: 'Efeitos' },
];

export function StyleEditor({
  styles = {},
  onChange,
  showDeviceTabs = true,
  className,
}: StyleEditorProps) {
  const [activeDevice, setActiveDevice] = useState<DeviceType>('desktop');
  const [expandedCategories, setExpandedCategories] = useState<StyleCategory[]>(['typography', 'spacing']);

  const currentStyles = styles[activeDevice] || {};

  const updateStyle = (property: string, value: string) => {
    const newStyles = {
      ...styles,
      [activeDevice]: {
        ...currentStyles,
        [property]: value || undefined,
      },
    };

    // Remove propriedades vazias
    if (!value) {
      delete (newStyles[activeDevice] as Record<string, unknown>)[property];
    }

    onChange(newStyles);
  };

  const toggleCategory = (category: StyleCategory) => {
    setExpandedCategories((prev) =>
      prev.includes(category)
        ? prev.filter((c) => c !== category)
        : [...prev, category]
    );
  };

  return (
    <div className={cn('space-y-4', className)}>
      {/* Device Tabs */}
      {showDeviceTabs && (
        <div className="flex items-center gap-1 p-1 bg-secondary rounded-lg">
          {(['desktop', 'tablet', 'mobile'] as DeviceType[]).map((device) => {
            const Icon = deviceIcons[device];
            return (
              <button
                key={device}
                onClick={() => setActiveDevice(device)}
                className={cn(
                  'flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-all',
                  activeDevice === device
                    ? 'bg-white dark:bg-neutral-800 text-primary shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                )}
              >
                <Icon size={14} />
                <span className="hidden sm:inline capitalize">{device}</span>
              </button>
            );
          })}
        </div>
      )}

      {/* Style Categories */}
      <div className="space-y-2">
        {styleCategories.map((category) => {
          const isExpanded = expandedCategories.includes(category.key);

          return (
            <div key={category.key} className="border rounded-lg overflow-hidden">
              <button
                onClick={() => toggleCategory(category.key)}
                className="w-full flex items-center justify-between px-3 py-2 bg-secondary/50 hover:bg-secondary transition-colors"
              >
                <span className="text-sm font-medium">{category.label}</span>
                <ChevronDown
                  size={16}
                  className={cn('transition-transform', isExpanded && 'rotate-180')}
                />
              </button>

              {isExpanded && (
                <div className="p-3 space-y-3">
                  {category.key === 'typography' && (
                    <TypographyFields
                      styles={currentStyles}
                      onChange={updateStyle}
                    />
                  )}
                  {category.key === 'spacing' && (
                    <SpacingFields
                      styles={currentStyles}
                      onChange={updateStyle}
                    />
                  )}
                  {category.key === 'layout' && (
                    <LayoutFields
                      styles={currentStyles}
                      onChange={updateStyle}
                    />
                  )}
                  {category.key === 'background' && (
                    <BackgroundFields
                      styles={currentStyles}
                      onChange={updateStyle}
                    />
                  )}
                  {category.key === 'border' && (
                    <BorderFields
                      styles={currentStyles}
                      onChange={updateStyle}
                    />
                  )}
                  {category.key === 'effects' && (
                    <EffectsFields
                      styles={currentStyles}
                      onChange={updateStyle}
                    />
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// === Field Components ===

interface FieldProps {
  styles: React.CSSProperties;
  onChange: (property: string, value: string) => void;
}

function TypographyFields({ styles, onChange }: FieldProps) {
  return (
    <>
      <StyleField
        label="Tamanho da fonte"
        value={styles.fontSize as string}
        onChange={(v) => onChange('fontSize', v)}
        placeholder="16px"
      />
      <StyleField
        label="Peso da fonte"
        value={styles.fontWeight as string}
        onChange={(v) => onChange('fontWeight', v)}
        type="select"
        options={[
          { value: '300', label: 'Light' },
          { value: '400', label: 'Normal' },
          { value: '500', label: 'Medium' },
          { value: '600', label: 'Semi Bold' },
          { value: '700', label: 'Bold' },
          { value: '800', label: 'Extra Bold' },
        ]}
      />
      <StyleField
        label="Cor do texto"
        value={styles.color as string}
        onChange={(v) => onChange('color', v)}
        type="color"
      />
      <StyleField
        label="Alinhamento"
        value={styles.textAlign as string}
        onChange={(v) => onChange('textAlign', v)}
        type="select"
        options={[
          { value: 'left', label: 'Esquerda' },
          { value: 'center', label: 'Centro' },
          { value: 'right', label: 'Direita' },
          { value: 'justify', label: 'Justificado' },
        ]}
      />
      <StyleField
        label="Altura da linha"
        value={styles.lineHeight as string}
        onChange={(v) => onChange('lineHeight', v)}
        placeholder="1.5"
      />
      <StyleField
        label="Espaçamento de letras"
        value={styles.letterSpacing as string}
        onChange={(v) => onChange('letterSpacing', v)}
        placeholder="0px"
      />
    </>
  );
}

function SpacingFields({ styles, onChange }: FieldProps) {
  return (
    <>
      <StyleField
        label="Margem"
        value={styles.margin as string}
        onChange={(v) => onChange('margin', v)}
        placeholder="0px"
      />
      <StyleField
        label="Padding"
        value={styles.padding as string}
        onChange={(v) => onChange('padding', v)}
        placeholder="0px"
      />
      <StyleField
        label="Gap"
        value={styles.gap as string}
        onChange={(v) => onChange('gap', v)}
        placeholder="0px"
      />
    </>
  );
}

function LayoutFields({ styles, onChange }: FieldProps) {
  return (
    <>
      <StyleField
        label="Display"
        value={styles.display as string}
        onChange={(v) => onChange('display', v)}
        type="select"
        options={[
          { value: 'block', label: 'Block' },
          { value: 'flex', label: 'Flex' },
          { value: 'grid', label: 'Grid' },
          { value: 'inline', label: 'Inline' },
          { value: 'inline-flex', label: 'Inline Flex' },
          { value: 'none', label: 'Oculto' },
        ]}
      />
      <StyleField
        label="Flex Direction"
        value={styles.flexDirection as string}
        onChange={(v) => onChange('flexDirection', v)}
        type="select"
        options={[
          { value: 'row', label: 'Linha' },
          { value: 'column', label: 'Coluna' },
          { value: 'row-reverse', label: 'Linha reversa' },
          { value: 'column-reverse', label: 'Coluna reversa' },
        ]}
      />
      <StyleField
        label="Justify Content"
        value={styles.justifyContent as string}
        onChange={(v) => onChange('justifyContent', v)}
        type="select"
        options={[
          { value: 'flex-start', label: 'Início' },
          { value: 'center', label: 'Centro' },
          { value: 'flex-end', label: 'Fim' },
          { value: 'space-between', label: 'Space Between' },
          { value: 'space-around', label: 'Space Around' },
        ]}
      />
      <StyleField
        label="Align Items"
        value={styles.alignItems as string}
        onChange={(v) => onChange('alignItems', v)}
        type="select"
        options={[
          { value: 'flex-start', label: 'Início' },
          { value: 'center', label: 'Centro' },
          { value: 'flex-end', label: 'Fim' },
          { value: 'stretch', label: 'Esticar' },
        ]}
      />
      <StyleField
        label="Largura"
        value={styles.width as string}
        onChange={(v) => onChange('width', v)}
        placeholder="auto"
      />
      <StyleField
        label="Altura"
        value={styles.height as string}
        onChange={(v) => onChange('height', v)}
        placeholder="auto"
      />
      <StyleField
        label="Max Width"
        value={styles.maxWidth as string}
        onChange={(v) => onChange('maxWidth', v)}
        placeholder="none"
      />
    </>
  );
}

function BackgroundFields({ styles, onChange }: FieldProps) {
  return (
    <>
      <StyleField
        label="Cor de fundo"
        value={styles.backgroundColor as string}
        onChange={(v) => onChange('backgroundColor', v)}
        type="color"
      />
      <StyleField
        label="Imagem de fundo"
        value={styles.backgroundImage as string}
        onChange={(v) => onChange('backgroundImage', v)}
        placeholder="url(...)"
      />
      <StyleField
        label="Posição do fundo"
        value={styles.backgroundPosition as string}
        onChange={(v) => onChange('backgroundPosition', v)}
        placeholder="center"
      />
      <StyleField
        label="Tamanho do fundo"
        value={styles.backgroundSize as string}
        onChange={(v) => onChange('backgroundSize', v)}
        type="select"
        options={[
          { value: 'cover', label: 'Cover' },
          { value: 'contain', label: 'Contain' },
          { value: 'auto', label: 'Auto' },
        ]}
      />
    </>
  );
}

function BorderFields({ styles, onChange }: FieldProps) {
  return (
    <>
      <StyleField
        label="Border"
        value={styles.border as string}
        onChange={(v) => onChange('border', v)}
        placeholder="1px solid #ccc"
      />
      <StyleField
        label="Border Radius"
        value={styles.borderRadius as string}
        onChange={(v) => onChange('borderRadius', v)}
        placeholder="0px"
      />
      <StyleField
        label="Cor da borda"
        value={styles.borderColor as string}
        onChange={(v) => onChange('borderColor', v)}
        type="color"
      />
    </>
  );
}

function EffectsFields({ styles, onChange }: FieldProps) {
  return (
    <>
      <StyleField
        label="Box Shadow"
        value={styles.boxShadow as string}
        onChange={(v) => onChange('boxShadow', v)}
        placeholder="none"
      />
      <StyleField
        label="Opacidade"
        value={styles.opacity as string}
        onChange={(v) => onChange('opacity', v)}
        placeholder="1"
        type="number"
        min={0}
        max={1}
        step={0.1}
      />
      <StyleField
        label="Transição"
        value={styles.transition as string}
        onChange={(v) => onChange('transition', v)}
        placeholder="all 0.3s ease"
      />
    </>
  );
}

// === Generic Style Field ===

interface StyleFieldProps {
  label: string;
  value: string | undefined;
  onChange: (value: string) => void;
  type?: 'text' | 'select' | 'color' | 'number';
  placeholder?: string;
  options?: { value: string; label: string }[];
  min?: number;
  max?: number;
  step?: number;
}

function StyleField({
  label,
  value,
  onChange,
  type = 'text',
  placeholder,
  options,
  min,
  max,
  step,
}: StyleFieldProps) {
  const inputClasses =
    'w-full px-2 py-1.5 text-sm border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-primary/50';

  return (
    <div className="flex items-center gap-2">
      <label className="text-xs text-muted-foreground w-28 shrink-0">{label}</label>

      {type === 'select' && options ? (
        <select
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          className={inputClasses}
        >
          <option value="">Selecione...</option>
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      ) : type === 'color' ? (
        <div className="flex items-center gap-2 flex-1">
          <input
            type="color"
            value={value || '#000000'}
            onChange={(e) => onChange(e.target.value)}
            className="w-8 h-8 rounded cursor-pointer"
          />
          <input
            type="text"
            value={value || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder="#000000"
            className={cn(inputClasses, 'flex-1')}
          />
        </div>
      ) : type === 'number' ? (
        <input
          type="number"
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          min={min}
          max={max}
          step={step}
          className={inputClasses}
        />
      ) : (
        <input
          type="text"
          value={value || ''}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className={inputClasses}
        />
      )}
    </div>
  );
}
