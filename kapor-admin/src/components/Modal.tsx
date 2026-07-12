import React from 'react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
}

export const Modal: React.FC<ModalProps> = ({ isOpen, onClose, title, children }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-md">
          <h3>{title}</h3>
          <button onClick={onClose} className="btn-ghost" style={{ padding: '4px' }}>
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
};
