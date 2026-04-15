import { PageSection } from "../../../../../utils/interfaces";

export const defaultMenuPaginas: PageSection[] = [
  {
    id: 'default',
    label: 'Padrão',
    description: 'Elementos que aparecem em todas as páginas',
    subSessoes: [
      {
        id: 'header',
        label: 'Menu',
        description: 'Barra de navegação superior',
      },
      {
        id: 'footer',
        label: 'Rodapé',
        description: 'Rodapé com informações de contato',
      }
    ],
  },
  {
    id: 'home',
    label: 'Início',
    description: 'Página inicial do site',
    subSessoes: [
      {
        id: 'hero',
        label: 'Apresentação',
        description: 'Seção principal de destaque',
      },
      {
        id: 'about',
        label: 'Sobre',
        description: 'Informações sobre a empresa',
      },
      {
        id: 'service',
        label: 'Serviços',
        description: 'Lista de serviços oferecidos',
      },
      {
        id: 'plan',
        label: 'Planos',
        description: 'Tabela de preços e planos',
        isNew: true,
      },
      {
        id: 'depoiment',
        label: 'Depoimentos',
        description: 'Avaliações de clientes',
      },
      {
        id: 'products',
        label: 'Produtos',
        description: 'Vitrine de produtos',
      },
      {
        id: 'agenda',
        label: 'Agendamento',
        description: 'Sistema de agendamento online',
        isNew: true,
      },
    ],
  },
  {
    id: 'services',
    label: 'Serviços',
    description: 'Página detalhada de serviços',
    subSessoes: [],
  },
  {
    id: 'products',
    label: 'Produtos',
    description: 'Catálogo de produtos',
    subSessoes: [],
  },
  {
    id: 'plans',
    label: 'Planos',
    description: 'Comparativo de planos',
    subSessoes: [],
  },
  {
    id: 'about',
    label: 'Sobre',
    description: 'História e valores da empresa',
    subSessoes: [],
  },
  {
    id: 'carShop',
    label: 'Carrinho',
    description: 'Carrinho de compras',
    subSessoes: [],
  },
];
