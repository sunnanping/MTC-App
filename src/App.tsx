import { useState, useEffect } from 'react';
import type { Website, FormMode } from './types';
import { getWebsites, saveWebsites, generateId } from './utils/storage';
import { NavBar } from './components/NavBar';
import { WebViewContainer } from './components/WebViewContainer';
import { WebsiteForm } from './components/WebsiteForm';
import { ContextMenu } from './components/ContextMenu';
import { DeleteConfirm } from './components/DeleteConfirm';

function App() {
  const [websites, setWebsites] = useState<Website[]>([]);
  const [activeWebsite, setActiveWebsite] = useState<Website | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formMode, setFormMode] = useState<FormMode>('add');
  const [editingWebsite, setEditingWebsite] = useState<Website | undefined>(undefined);
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    website: Website;
  } | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<Website | null>(null);

  useEffect(() => {
    const savedWebsites = getWebsites();
    setWebsites(savedWebsites);
    if (savedWebsites.length > 0) {
      setActiveWebsite(savedWebsites[0]);
    }
  }, []);

  const handleSelectWebsite = (website: Website) => {
    setActiveWebsite(website);
    setContextMenu(null);
  };

  const handleAddWebsite = () => {
    setFormMode('add');
    setEditingWebsite(undefined);
    setShowForm(true);
  };

  const handleEditWebsite = (website: Website) => {
    setFormMode('edit');
    setEditingWebsite(website);
    setShowForm(true);
  };

  const handleLongPress = (website: Website, e: React.TouchEvent | React.MouseEvent) => {
    e.preventDefault();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    setContextMenu({ x: clientX, y: clientY, website });
  };

  const handleFormSubmit = (data: Omit<Website, 'id' | 'order'> & { id?: string }) => {
    if (data.id) {
      const updated = websites.map((w) =>
        w.id === data.id
          ? { ...w, name: data.name, url: data.url, icon: data.icon, color: data.color }
          : w
      );
      setWebsites(updated);
      saveWebsites(updated);
      if (activeWebsite?.id === data.id) {
        setActiveWebsite({
          ...activeWebsite,
          name: data.name,
          url: data.url,
          icon: data.icon,
          color: data.color,
        });
      }
    } else {
      const newWebsite: Website = {
        id: generateId(),
        name: data.name,
        url: data.url,
        icon: data.icon,
        color: data.color,
        order: websites.length + 1,
      };
      const updated = [...websites, newWebsite];
      setWebsites(updated);
      saveWebsites(updated);
    }
    setShowForm(false);
  };

  const handleDeleteConfirm = (website: Website) => {
    setDeleteConfirm(website);
    setContextMenu(null);
  };

  const handleDelete = () => {
    if (!deleteConfirm) return;
    const updated = websites.filter((w) => w.id !== deleteConfirm.id);
    setWebsites(updated);
    saveWebsites(updated);
    if (activeWebsite?.id === deleteConfirm.id) {
      setActiveWebsite(updated[0] || null);
    }
    setDeleteConfirm(null);
  };

  return (
    <div className="w-full h-screen flex flex-col bg-gray-900">
      <NavBar
        websites={websites}
        activeId={activeWebsite?.id || ''}
        onSelect={handleSelectWebsite}
        onAdd={handleAddWebsite}
        onLongPress={handleLongPress}
      />

      <div className="flex-1 pt-16">
        {activeWebsite ? (
          <WebViewContainer website={activeWebsite} onError={() => {}} />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-gray-900">
            <div className="text-center px-4">
              <div className="text-6xl mb-4">🌐</div>
              <p className="text-gray-400">请添加或选择一个网站</p>
            </div>
          </div>
        )}
      </div>

      {showForm && (
        <WebsiteForm
          mode={formMode}
          website={editingWebsite}
          onSubmit={handleFormSubmit}
          onCancel={() => setShowForm(false)}
        />
      )}

      {contextMenu && (
        <ContextMenu
          x={contextMenu.x}
          y={contextMenu.y}
          onEdit={() => handleEditWebsite(contextMenu.website)}
          onDelete={() => handleDeleteConfirm(contextMenu.website)}
          onClose={() => setContextMenu(null)}
        />
      )}

      {deleteConfirm && (
        <DeleteConfirm
          websiteName={deleteConfirm.name}
          onConfirm={handleDelete}
          onCancel={() => setDeleteConfirm(null)}
        />
      )}
    </div>
  );
}

export default App;
