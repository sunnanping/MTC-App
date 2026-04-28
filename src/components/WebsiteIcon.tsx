import { useState, useRef, useEffect } from 'react';
import type { Website } from '../types';

interface WebsiteIconProps {
  website: Website;
  isActive: boolean;
  onClick: () => void;
  onLongPress: (e: React.TouchEvent | React.MouseEvent) => void;
}

export const WebsiteIcon = ({ website, isActive, onClick, onLongPress }: WebsiteIconProps) => {
  const [isPressed, setIsPressed] = useState(false);
  const longPressTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleMouseDown = () => {
    setIsPressed(true);
    longPressTimer.current = setTimeout(() => {
      const event = new MouseEvent('contextmenu');
      onLongPress(event);
    }, 500);
  };

  const handleMouseUp = () => {
    setIsPressed(false);
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
    }
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    setIsPressed(true);
    longPressTimer.current = setTimeout(() => {
      onLongPress(e);
    }, 500);
  };

  const handleTouchEnd = () => {
    setIsPressed(false);
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
    }
  };

  useEffect(() => {
    return () => {
      if (longPressTimer.current) {
        clearTimeout(longPressTimer.current);
      }
    };
  }, []);

  return (
    <div
      onClick={onClick}
      onMouseDown={handleMouseDown}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      className={`
        relative flex-shrink-0 w-12 h-12 rounded-full flex items-center justify-center
        text-white font-bold text-lg shadow-lg cursor-pointer
        transition-all duration-200 select-none
        ${isActive ? 'scale-110 ring-2 ring-white/50' : 'scale-100'}
        ${isPressed ? 'scale-95' : ''}
      `}
      style={{ backgroundColor: website.color }}
      title={website.name}
    >
      {website.icon}
      {isActive && (
        <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-white rounded-full border-2 border-gray-900" />
      )}
    </div>
  );
};
