import { useRef } from 'react';
import type { Website } from '../types';
import { WebsiteIcon } from './WebsiteIcon';

interface NavBarProps {
  websites: Website[];
  activeId: string;
  onSelect: (website: Website) => void;
  onAdd: () => void;
  onLongPress: (website: Website, e: React.TouchEvent | React.MouseEvent) => void;
}

export const NavBar = ({ websites, activeId, onSelect, onAdd, onLongPress }: NavBarProps) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  return (
    <div className="fixed top-0 left-0 right-0 z-40 bg-gray-900/95 backdrop-blur-md border-b border-gray-700">
      <div className="flex items-center justify-between px-4 py-3">
        <div
          ref={scrollRef}
          className="flex gap-3 overflow-x-auto scrollbar-hide flex-1 max-w-[calc(100%-60px)]"
        >
          {websites.map((website) => (
            <WebsiteIcon
              key={website.id}
              website={website}
              isActive={website.id === activeId}
              onClick={() => onSelect(website)}
              onLongPress={(e) => onLongPress(website, e)}
            />
          ))}
        </div>

        <button
          onClick={onAdd}
          className="w-12 h-12 rounded-full bg-gray-700 hover:bg-gray-600 flex items-center justify-center text-white text-2xl transition-colors flex-shrink-0 ml-2"
          title="添加网站"
        >
          +
        </button>
      </div>
    </div>
  );
};
