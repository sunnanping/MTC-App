import { useEffect, useRef, useState } from 'react';
import type { Website } from '../types';

interface WebViewContainerProps {
  website: Website;
  onError: () => void;
}

export const WebViewContainer = ({ website, onError }: WebViewContainerProps) => {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setIsLoading(true);
    setError(null);
  }, [website.url]);

  const handleLoad = () => {
    setIsLoading(false);
    setError(null);
  };

  const handleError = () => {
    setIsLoading(false);
    setError('无法加载此网站');
    onError();
  };

  return (
    <div className="flex-1 relative bg-white">
      {isLoading && (
        <div className="absolute inset-0 bg-gray-900 flex items-center justify-center z-10">
          <div className="flex flex-col items-center gap-4">
            <div className="w-12 h-12 rounded-full border-4 border-primary border-t-transparent animate-spin" />
            <span className="text-gray-400">加载中...</span>
          </div>
        </div>
      )}

      {error && (
        <div className="absolute inset-0 bg-gray-900 flex items-center justify-center z-10">
          <div className="text-center px-4">
            <div className="text-6xl mb-4">🔴</div>
            <p className="text-gray-400">{error}</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-4 px-4 py-2 bg-primary text-white rounded-lg"
            >
              重试
            </button>
          </div>
        </div>
      )}

      <iframe
        ref={iframeRef}
        src={website.url}
        title={website.name}
        className="w-full h-full border-none"
        onLoad={handleLoad}
        onError={handleError}
        sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-top-navigation"
        allow="geolocation; microphone; camera"
      />
    </div>
  );
};
