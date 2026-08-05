import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { cn } from '../../utils';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import type { User } from '../../types';

type Role = User['role'];

const icons: Record<string, string> = {
  wallet: '💰',
  transfer: '💸',
  requestMoney: '📩',
  bills: '📄',
  airtime: '📱',
  merchant: '🏪',
  becomeMerchant: '🏪',
  merchantAnalytics: '📊',
  merchantReports: '🧮',
  merchantPromotions: '🎉',
  paymentLinks: '🔗',
  terminal: '🖥️',
  dynamicQr: '🔳',
  bulkOps: '🧹',
  statements: '🧾',
  stores: '🏬',
  chargebacks: '⚖️',
  financing: '💵',
  riskAlerts: '🚨',
  reconciliation: '📊',
  merchantDashboard: '📈',
  invoices: '🧾',
  inventory: '📦',
  agent: '🤝',
  corporate: '🏢',
  admin: '⚙️',
  adminKyc: '🪪',
  adminAml: '🚨',
  adminUsers: '👤',
  adminMerchants: '🏬',
  adminRefData: '🗂️',
  disputes: '⚖️',
  audit: '📋',
  settlements: '🏦',
  savings: '🐷',
  directory: '🗺️',
  scheduled: '⏰',
  staff: '👥',
  payroll: '💳',
  remittance: '🌏',
  promotions: '🎉',
  support: '🎧',
};

interface NavItem {
  key: string;
  route: string;
  roles: Role[];
  section?: string;
  group?: string;
}

const GROUP_ORDER = ['overview', 'payments', 'business', 'risk', 'management', 'support'];

const navItems: NavItem[] = [
  { key: 'wallet', route: '/wallet', roles: ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'] },
  { key: 'transfer', route: '/transfer', roles: ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'] },
  { key: 'requestMoney', route: '/request-money', roles: ['CONSUMER'] },
  { key: 'bills', route: '/bills', roles: ['CONSUMER'] },
  { key: 'airtime', route: '/airtime', roles: ['CONSUMER'] },
  { key: 'savings', route: '/savings', roles: ['CONSUMER'] },
  { key: 'scheduled', route: '/scheduled', roles: ['CONSUMER'] },
  { key: 'remittance', route: '/remittance', roles: ['CONSUMER'] },
  { key: 'promotions', route: '/promotions', roles: ['CONSUMER', 'MERCHANT'] },
  { key: 'directory', route: '/directory', roles: ['CONSUMER'] },

  { key: 'merchant', route: '/merchant', roles: ['MERCHANT'], group: 'overview' },
  { key: 'becomeMerchant', route: '/merchant', roles: ['CONSUMER'] },
  { key: 'merchantDashboard', route: '/merchant/dashboard', roles: ['MERCHANT'], group: 'overview' },
  { key: 'merchantAnalytics', route: '/merchant/analytics', roles: ['MERCHANT'], group: 'overview' },
  { key: 'merchantReports', route: '/merchant/reports', roles: ['MERCHANT'], group: 'overview' },
  { key: 'merchantPromotions', route: '/merchant/promotions', roles: ['MERCHANT'], group: 'business' },
  { key: 'paymentLinks', route: '/merchant/payment-links', roles: ['MERCHANT'], group: 'payments' },
  { key: 'terminal', route: '/merchant/terminal', roles: ['MERCHANT'], group: 'payments' },
  { key: 'dynamicQr', route: '/merchant/qr', roles: ['MERCHANT'], group: 'payments' },
  { key: 'bulkOps', route: '/merchant/bulk', roles: ['MERCHANT'], group: 'payments' },
  { key: 'invoices', route: '/invoices', roles: ['MERCHANT'], group: 'payments' },
  { key: 'statements', route: '/merchant/statements', roles: ['MERCHANT'], group: 'payments' },
  { key: 'reconciliation', route: '/merchant/reconciliation', roles: ['MERCHANT'], group: 'payments' },
  { key: 'stores', route: '/merchant/stores', roles: ['MERCHANT'], group: 'business' },
  { key: 'inventory', route: '/inventory', roles: ['MERCHANT'], group: 'business' },
  { key: 'financing', route: '/merchant/financing', roles: ['MERCHANT'], group: 'business' },
  { key: 'chargebacks', route: '/merchant/chargebacks', roles: ['MERCHANT'], group: 'risk' },
  { key: 'riskAlerts', route: '/merchant/risk-alerts', roles: ['MERCHANT'], group: 'risk' },
  { key: 'disputes', route: '/disputes', roles: ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'], group: 'risk' },
  { key: 'staff', route: '/staff', roles: ['MERCHANT'], group: 'management' },
  { key: 'settlements', route: '/settlements', roles: ['MERCHANT'], group: 'management' },

  { key: 'agent', route: '/agent', roles: ['AGENT'] },

  { key: 'corporate', route: '/corporate', roles: ['CORPORATE'] },
  { key: 'payroll', route: '/payroll', roles: ['CORPORATE'] },

  { key: 'support', route: '/support', roles: ['CONSUMER', 'MERCHANT', 'AGENT', 'CORPORATE', 'ADMIN'], group: 'support' },

  { key: 'admin', route: '/admin', roles: ['ADMIN'] },
  { key: 'adminKyc', route: '/admin/kyc', roles: ['ADMIN'] },
  { key: 'adminAml', route: '/admin/aml', roles: ['ADMIN'] },
  { key: 'adminUsers', route: '/admin/users', roles: ['ADMIN'] },
  { key: 'adminMerchants', route: '/admin/merchants', roles: ['ADMIN'] },
  { key: 'adminRefData', route: '/admin/refdata', roles: ['ADMIN'] },
  { key: 'audit', route: '/audit', roles: ['ADMIN'] },
];

const roleLabels: Record<Role, string> = {
  CONSUMER: 'Consumer',
  MERCHANT: 'Merchant',
  AGENT: 'Agent',
  CORPORATE: 'Corporate',
  ADMIN: 'Admin',
};

export function Sidebar() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const role = user?.role ?? 'CONSUMER';
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(() => {
    const initial: Record<string, boolean> = {};
    GROUP_ORDER.forEach((g, i) => {
      initial[g] = i === 0;
    });
    return initial;
  });

  const visibleItems = navItems.filter((item) => item.roles.includes(role));
  const flatItems = visibleItems.filter((item) => !item.group);
  const grouped = GROUP_ORDER
    .map((g) => ({ group: g, items: visibleItems.filter((item) => item.group === g) }))
    .filter((g) => g.items.length > 0);

  const toggleGroup = (group: string) =>
    setOpenGroups((prev) => ({ ...prev, [group]: !prev[group] }));

  const renderItem = (item: NavItem) => (
    <NavLink
      key={item.key}
      to={item.route}
      className={({ isActive }) =>
        cn(
          'flex items-center space-x-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
          isActive ? 'bg-blue-600 text-white' : 'text-gray-300 hover:bg-gray-800 hover:text-white'
        )
      }
    >
      <span className="text-base">{icons[item.key]}</span>
      <span>{t.nav[item.key as keyof typeof t.nav]}</span>
    </NavLink>
  );

  return (
    <aside className="w-64 bg-gray-900 text-white min-h-screen p-4 overflow-y-auto">
      <div className="mb-8">
        <h1 className="text-xl font-bold">{t.common.appName}</h1>
        <p className="text-xs text-gray-400 mt-1">{roleLabels[role]} Portal</p>
      </div>

      <nav className="space-y-1">
        {flatItems.map(renderItem)}

        {grouped.map(({ group, items }) => {
          const isOpen = !!openGroups[group];
          return (
            <div key={group}>
              <button
                onClick={() => toggleGroup(group)}
                className="w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold uppercase tracking-wide text-gray-400 hover:bg-gray-800 hover:text-white transition-colors"
              >
                <span>{t.nav[`group${group.charAt(0).toUpperCase()}${group.slice(1)}` as keyof typeof t.nav]}</span>
                <svg
                  className={cn('w-4 h-4 transition-transform', isOpen && 'rotate-180')}
                  fill="none" viewBox="0 0 24 24" stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              {isOpen && <div className="mt-1 space-y-1">{items.map(renderItem)}</div>}
            </div>
          );
        })}
      </nav>
    </aside>
  );
}
