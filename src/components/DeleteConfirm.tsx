interface DeleteConfirmProps {
  websiteName: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export const DeleteConfirm = ({ websiteName, onConfirm, onCancel }: DeleteConfirmProps) => {
  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-gray-900 rounded-2xl w-full max-w-sm shadow-2xl">
        <div className="p-4 border-b border-gray-700">
          <h2 className="text-lg font-semibold text-white">确认删除</h2>
        </div>

        <div className="p-4">
          <p className="text-gray-300 mb-4">
            确定要删除网站 <span className="text-primary font-semibold">{websiteName}</span> 吗？
          </p>

          <div className="flex gap-3">
            <button
              onClick={onCancel}
              className="flex-1 px-4 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
            >
              取消
            </button>
            <button
              onClick={onConfirm}
              className="flex-1 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-400 transition-colors"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
