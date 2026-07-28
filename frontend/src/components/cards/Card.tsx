import { cn } from '../../utils';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  title?: string;
  subtitle?: string;
}

export function Card({ children, className, title, subtitle }: CardProps) {
  return (
    <div className={cn('bg-white rounded-xl shadow-sm border border-gray-200 p-6', className)}>
      {(title || subtitle) && (
        <div className="mb-4">
          {title && <h3 className="text-lg font-semibold text-gray-900">{title}</h3>}
          {subtitle && <p className="text-sm text-gray-500 mt-1">{subtitle}</p>}
        </div>
      )}
      {children}
    </div>
  );
}
