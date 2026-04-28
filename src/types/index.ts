export interface Website {
  id: string;
  name: string;
  url: string;
  icon: string;
  color: string;
  order: number;
}

export type FormMode = 'add' | 'edit';
