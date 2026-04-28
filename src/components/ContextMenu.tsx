import { useEffect, useRef } from 'react';

interface ContextMenuProps {
  x: number;
  y: number;
  onEdit: () => void;
  onDelete: () => void;
  onClose: () => void;
}

export const ContextMenu = ({ x, y, onEdit, onDelete, onClose }: ContextMenuProps) => {
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose();
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, [onClose]);

  return (
    <div
      ref={menuRef}
      className="fixed bg-gray-800 rounded-xl shadow-xl py-2 min-w-[140px] z-50 border border-gray-700"
      style={{
        left: Math.min(x, window.innerWidth - 140),
        top: Math.min(y, window.innerHeight - 200),
      }}
    >
      <button
        onClick={() => { onEdit(); onClose(); }}
        className="w-full px-4 py-2 text-left text-white hover:bg-gray-700 transition-colors flex items-center gap-2"
      >
        <span className="text-lg">✏️</span>
        <span>编辑</span>
      </button>
      <button
        onClick={() => { onDelete(); onClose(); }}
        className="w-full px-4 py-2 text-left text-red-400 hover:bg-gray-700 transition-colors flex items-center gap-2"
      >
        <span className="text-lg">🗑️</span>
        <span>删除</span>
      </button>
    </div>
  );
};
