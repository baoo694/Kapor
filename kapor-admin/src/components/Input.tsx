import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
}

export const Input: React.FC<InputProps> = ({ label, className = '', ...props }) => {
  return (
    <div className="flex-col gap-sm w-full">
      {label && <label className="text-sm font-semibold">{label}</label>}
      <input className={`input ${className}`} {...props} />
    </div>
  );
};
