import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { invoiceApi } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatDate, cn } from '../../utils';
import type { Invoice, InvoiceItem } from '../../types';

export function InvoicesPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [showDetail, setShowDetail] = useState<Invoice | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [customerPhone, setCustomerPhone] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [items, setItems] = useState<Omit<InvoiceItem, 'id'>[]>([{ name: '', quantity: 1, price: 0 }]);
  const [tax, setTax] = useState(0);
  const [dueDate, setDueDate] = useState('');

  const loadInvoices = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await invoiceApi.getByMerchant(user.id);
      setInvoices(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInvoices();
  }, [user]);

  const subtotal = items.reduce((sum, item) => sum + item.quantity * item.price, 0);
  const total = subtotal + tax;

  const handleAddItem = () => {
    setItems([...items, { name: '', quantity: 1, price: 0 }]);
  };

  const handleRemoveItem = (index: number) => {
    if (items.length === 1) return;
    setItems(items.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: keyof Omit<InvoiceItem, 'id'>, value: string | number) => {
    const updated = [...items];
    (updated[index] as Record<string, unknown>)[field] = value;
    setItems(updated);
  };

  const handleCreate = async () => {
    if (!user || !customerPhone || !customerName || items.length === 0 || !dueDate) return;
    setSubmitting(true);
    try {
      await invoiceApi.create(user.id, {
        customerPhone,
        customerName,
        items,
        tax,
        dueDate,
      });
      setShowCreate(false);
      resetForm();
      await loadInvoices();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setCustomerPhone('');
    setCustomerName('');
    setItems([{ name: '', quantity: 1, price: 0 }]);
    setTax(0);
    setDueDate('');
  };

  const handleSend = async (invoiceId: string) => {
    if (!user) return;
    setSubmitting(true);
    try {
      await invoiceApi.send(user.id, invoiceId);
      await loadInvoices();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleMarkPaid = async (invoiceId: string) => {
    if (!user) return;
    setSubmitting(true);
    try {
      await invoiceApi.markPaid(user.id, invoiceId);
      await loadInvoices();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async (invoiceId: string) => {
    if (!user) return;
    setSubmitting(true);
    try {
      await invoiceApi.cancel(user.id, invoiceId);
      await loadInvoices();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const statusColor = (s: string) => {
    const m: Record<string, string> = {
      DRAFT: 'bg-gray-100 text-gray-800',
      SENT: 'bg-blue-100 text-blue-800',
      PAID: 'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800',
    };
    return m[s] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">{t.invoice.title}</h1>
        <Button onClick={() => setShowCreate(true)}>{t.invoice.createInvoice}</Button>
      </div>

      {loading ? (
        <div className="text-center py-8">{t.common.loading}</div>
      ) : invoices.length === 0 ? (
        <Card>
          <p className="text-center text-gray-500 py-8">{t.invoice.noInvoices}</p>
        </Card>
      ) : (
        <div className="space-y-2">
          {invoices.map((inv) => (
            <div key={inv.id} className="bg-white border border-gray-200 rounded-xl p-4 hover:bg-gray-50 transition-colors">
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm font-medium text-gray-900">{inv.customerName}</span>
                    <span className={cn('text-xs px-2 py-0.5 rounded-full', statusColor(inv.status))}>
                      {t.invoice.status[inv.status as keyof typeof t.invoice.status]}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500">{inv.total.toLocaleString()} MMK &middot; {inv.customerPhone}</p>
                  <p className="text-xs text-gray-400">Due: {formatDate(inv.dueDate)}</p>
                </div>
                <div className="flex space-x-2">
                  <Button size="sm" variant="ghost" onClick={() => setShowDetail(inv)}>
                    {t.common.viewDetails}
                  </Button>
                  {inv.status === 'DRAFT' && (
                    <Button size="sm" onClick={() => handleSend(inv.id)} loading={submitting}>
                      {t.invoice.sendInvoice}
                    </Button>
                  )}
                  {inv.status === 'SENT' && (
                    <>
                      <Button size="sm" onClick={() => handleMarkPaid(inv.id)} loading={submitting}>
                        {t.invoice.markPaid}
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => handleCancel(inv.id)} loading={submitting}>
                        {t.invoice.cancelInvoice}
                      </Button>
                    </>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showCreate} onClose={() => setShowCreate(false)} title={t.invoice.createInvoice}>
        <div className="space-y-4">
          <Input
            label={t.invoice.customerPhone}
            placeholder={t.invoice.customerPhonePlaceholder}
            value={customerPhone}
            onChange={(e) => setCustomerPhone(e.target.value)}
          />
          <Input
            label={t.invoice.customerName}
            placeholder={t.invoice.customerNamePlaceholder}
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t.invoice.items}</label>
            <div className="space-y-2">
              {items.map((item, idx) => (
                <div key={idx} className="flex items-center space-x-2">
                  <input
                    placeholder={t.invoice.itemNamePlaceholder}
                    value={item.name}
                    onChange={(e) => handleItemChange(idx, 'name', e.target.value)}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  />
                  <input
                    type="number"
                    placeholder={t.invoice.quantity}
                    value={item.quantity}
                    onChange={(e) => handleItemChange(idx, 'quantity', Number(e.target.value))}
                    className="w-20 px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  />
                  <input
                    type="number"
                    placeholder={t.invoice.price}
                    value={item.price || ''}
                    onChange={(e) => handleItemChange(idx, 'price', Number(e.target.value))}
                    className="w-28 px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  />
                  {items.length > 1 && (
                    <button
                      type="button"
                      onClick={() => { handleItemChange(idx, 'name', ''); handleRemoveItem(idx); }}
                      className="text-red-500 hover:text-red-700 text-sm"
                    >
                      {t.invoice.removeItem}
                    </button>
                  )}
                </div>
              ))}
            </div>
            <Button size="sm" variant="ghost" onClick={handleAddItem} className="mt-2">
              + {t.invoice.addItem}
            </Button>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="text-gray-500">{t.invoice.subtotal}:</div>
            <div className="text-right">{subtotal.toLocaleString()} MMK</div>
            <div className="text-gray-500">{t.invoice.tax}:</div>
            <div className="text-right">
              <input
                type="number"
                value={tax || ''}
                onChange={(e) => setTax(Number(e.target.value))}
                className="w-full px-2 py-1 border border-gray-300 rounded text-sm text-right"
              />
            </div>
            <div className="font-medium">{t.invoice.total}:</div>
            <div className="text-right font-medium">{total.toLocaleString()} MMK</div>
          </div>

          <Input
            label={t.invoice.dueDate}
            type="date"
            placeholder={t.invoice.dueDatePlaceholder}
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
          />

          <div className="flex space-x-3">
            <Button onClick={handleCreate} loading={submitting} className="flex-1">{t.invoice.createInvoice}</Button>
            <Button variant="secondary" onClick={() => setShowCreate(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!showDetail} onClose={() => setShowDetail(null)} title={t.invoice.invoiceDetail}>
        {showDetail && (
          <div className="space-y-4">
            <div className="space-y-2 text-sm">
              <p><span className="font-medium">{t.invoice.customerName}:</span> {showDetail.customerName}</p>
              <p><span className="font-medium">{t.invoice.customerPhone}:</span> {showDetail.customerPhone}</p>
              <p><span className="font-medium">{t.common.status}:</span> {t.invoice.status[showDetail.status as keyof typeof t.invoice.status]}</p>
              <p><span className="font-medium">{t.invoice.dueDate}:</span> {formatDate(showDetail.dueDate)}</p>
            </div>
            <div>
              <h4 className="font-medium text-gray-900 mb-2">{t.invoice.items}</h4>
              <div className="bg-gray-50 rounded-lg">
                {showDetail.items.map((item) => (
                  <div key={item.id} className="flex justify-between px-3 py-2 border-b last:border-b-0 text-sm">
                    <span>{item.name} x {item.quantity}</span>
                    <span>{(item.quantity * item.price).toLocaleString()} MMK</span>
                  </div>
                ))}
              </div>
              <div className="mt-2 space-y-1 text-sm">
                <div className="flex justify-between"><span>{t.invoice.subtotal}</span><span>{showDetail.subtotal.toLocaleString()} MMK</span></div>
                <div className="flex justify-between"><span>{t.invoice.tax}</span><span>{showDetail.tax.toLocaleString()} MMK</span></div>
                <div className="flex justify-between font-medium"><span>{t.invoice.total}</span><span>{showDetail.total.toLocaleString()} MMK</span></div>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
