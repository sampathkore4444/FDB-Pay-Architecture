import { formatCurrency, formatDate, cn } from '../../utils';
import type { Transaction } from '../../types';

interface TransactionListProps {
  transactions: Transaction[];
  loading?: boolean;
}

const statusColors: Record<string, string> = {
  COMPLETED: 'bg-green-100 text-green-800',
  PENDING: 'bg-yellow-100 text-yellow-800',
  FAILED: 'bg-red-100 text-red-800',
  REVERSED: 'bg-orange-100 text-orange-800',
  CANCELLED: 'bg-gray-100 text-gray-800',
};

export function TransactionList({ transactions, loading }: TransactionListProps) {
  if (loading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="animate-pulse flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div className="space-y-2">
              <div className="h-4 bg-gray-200 rounded w-32" />
              <div className="h-3 bg-gray-200 rounded w-24" />
            </div>
            <div className="h-4 bg-gray-200 rounded w-20" />
          </div>
        ))}
      </div>
    );
  }

  if (transactions.length === 0) {
    return <p className="text-center text-gray-500 py-8">No transactions found</p>;
  }

  return (
    <div className="space-y-2">
      {transactions.map((txn) => (
        <div key={txn.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
          <div className="flex items-center space-x-3">
            <div className={cn('w-10 h-10 rounded-full flex items-center justify-center text-sm font-medium',
              txn.type.includes('BILL') ? 'bg-purple-100 text-purple-700' :
              txn.type.includes('MERCHANT') ? 'bg-blue-100 text-blue-700' :
              'bg-green-100 text-green-700'
            )}>
              {txn.type.charAt(0)}
            </div>
            <div>
              <p className="text-sm font-medium text-gray-900">{txn.description || txn.type}</p>
              <p className="text-xs text-gray-500">{formatDate(txn.createdAt)}</p>
            </div>
          </div>
          <div className="text-right">
            <p className={cn('text-sm font-semibold',
              txn.type.includes('DEBIT') ? 'text-red-600' : 'text-green-600'
            )}>
              {txn.type.includes('DEBIT') ? '-' : '+'}{formatCurrency(txn.amount)}
            </p>
            <span className={cn('inline-block px-2 py-0.5 text-xs rounded-full', statusColors[txn.status])}>
              {txn.status}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}
