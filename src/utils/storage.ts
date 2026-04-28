import type { Website } from '../types';

const STORAGE_KEY = 'mtc_websites';

const initialWebsites: Website[] = [
  { id: '1', name: 'TRAE SOLO', url: 'https://solo.trae.cn', icon: 'T', color: '#5C61FF', order: 1 },
  { id: '2', name: 'DeepSeek', url: 'https://chat.deepseek.com', icon: 'D', color: '#00D4AA', order: 2 },
  { id: '3', name: '豆包', url: 'https://www.doubao.com/chat', icon: '豆', color: '#1890FF', order: 3 },
  { id: '4', name: 'Kimi', url: 'https://www.kimi.com', icon: 'K', color: '#FF6B6B', order: 4 },
  { id: '5', name: 'NotebookLM', url: 'https://notebooklm.google.com', icon: 'N', color: '#4285F4', order: 5 },
];

export const getWebsites = (): Website[] => {
  try {
    const data = localStorage.getItem(STORAGE_KEY);
    if (data) {
      return JSON.parse(data);
    }
  } catch (e) {
    console.error('Failed to load websites:', e);
  }
  saveWebsites(initialWebsites);
  return initialWebsites;
};

export const saveWebsites = (websites: Website[]): void => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(websites));
  } catch (e) {
    console.error('Failed to save websites:', e);
  }
};

export const generateId = (): string => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2);
};

export const generateColor = (): string => {
  const colors = [
    '#5C61FF', '#00D4AA', '#1890FF', '#FF6B6B', '#4285F4',
    '#FF9800', '#E91E63', '#9C27B0', '#00BCD4', '#8BC34A'
  ];
  return colors[Math.floor(Math.random() * colors.length)];
};

export const getIconFromName = (name: string): string => {
  if (name.length === 0) return '?';
  const chineseRegex = /[\u4e00-\u9fa5]/;
  if (chineseRegex.test(name)) {
    return name.charAt(0);
  }
  return name.charAt(0).toUpperCase();
};

export const validateUrl = (url: string): boolean => {
  try {
    const urlObj = new URL(url);
    return ['http:', 'https:'].includes(urlObj.protocol);
  } catch {
    return false;
  }
};
