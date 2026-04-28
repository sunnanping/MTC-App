import { useState, useEffect } from 'react';
import type { Website, FormMode } from '../types';
import { generateColor, getIconFromName, validateUrl } from '../utils/storage';

interface WebsiteFormProps {
  mode: FormMode;
  website?: Website;
  onSubmit: (website: Omit<Website, 'id' | 'order'> & { id?: string }) => void;
  onCancel: () => void;
}

export const WebsiteForm = ({ mode, website, onSubmit, onCancel }: WebsiteFormProps) => {
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (mode === 'edit' && website) {
      setName(website.name);
      setUrl(website.url);
    }
  }, [mode, website]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!name.trim()) {
      setError('请输入网站名称');
      return;
    }

    if (!url.trim()) {
      setError('请输入网站地址');
      return;
    }

    if (!validateUrl(url)) {
      setError('请输入有效的网址（以 http:// 或 https:// 开头）');
      return;
    }

    onSubmit({
      id: website?.id,
      name: name.trim(),
      url: url.trim(),
      icon: getIconFromName(name.trim()),
      color: website?.color || generateColor(),
    });
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-gray-900 rounded-2xl w-full max-w-sm shadow-2xl">
        <div className="flex items-center justify-between p-4 border-b border-gray-700">
          <h2 className="text-lg font-semibold text-white">
            {mode === 'add' ? '添加网站' : '编辑网站'}
          </h2>
          <button
            onClick={onCancel}
            className="text-gray-400 hover:text-white transition-colors text-xl leading-none"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          <div>
            <label className="block text-sm text-gray-400 mb-1">网站名称</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary"
              placeholder="输入网站名称"
              maxLength={20}
            />
          </div>

          <div>
            <label className="block text-sm text-gray-400 mb-1">网站地址</label>
            <input
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              className="w-full px-4 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary"
              placeholder="https://example.com"
            />
          </div>

          {error && (
            <p className="text-red-400 text-sm">{error}</p>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onCancel}
              className="flex-1 px-4 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              className="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/80 transition-colors"
            >
              {mode === 'add' ? '添加' : '保存'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
