import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { orderApi, refundApi, catalogApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency, formatDate } from '../../utils';
import type { MerchantOrder, MerchantOrderItem, Product, ProductVariant } from '../../types';

interface OrderItemDraft {
  key: number;
  productId: string;
  productName: string;
  quantity: string;
  unitPrice: string;
  variantId?: string;
  variants: ProductVariant[];
}

const emptyRefundForm = { amount: '', reason: '', requireApproval: false };

export function OrdersPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [orders, setOrders] = useState<MerchantOrder[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [showOrderForm, setShowOrderForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [customerPhone, setCustomerPhone] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [taxRate, setTaxRate] = useState('');
  const [items, setItems] = useState<OrderItemDraft[]>([]);
  const [nextKey, setNextKey] = useState(1);
  const [refundFor, setRefundFor] = useState<MerchantOrder | null>(null);
  const [refundForm, setRefundForm] = useState(emptyRefundForm);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [o, p] = await Promise.all([orderApi.list(user.id, statusFilter || undefined), catalogApi.list(user.id)]);
      setOrders(o);
      setProducts(p);
    } catch (err) {
      console.error('Failed to load orders', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user, statusFilter]);

  const loadVariants = async (productId: string) => {
    if (!user) return [];
    try {
      return await catalogApi.listVariants(user.id, productId);
    } catch {
      return [];
    }
  };

  const openCreate = () => {
    setCustomerPhone('');
    setCustomerName('');
    setTaxRate('');
    setItems([]);
    setNextKey(1);
    setShowOrderForm(true);
  };

  const addItem = () => {
    setItems((prev) => [...prev, { key: nextKey, productId: '', productName: '', quantity: '1', unitPrice: '', variants: [] }]);
    setNextKey((k) => k + 1);
  };

  const onProductSelect = async (key: number, productId: string) => {
    const product = products.find((p) => p.id === productId);
    if (!product) return;
    const variants = await loadVariants(productId);
    setItems((prev) =>
      prev.map((it) =>
        it.key === key
          ? { ...it, productId, productName: product.name, unitPrice: String(product.price), quantity: it.quantity || '1', variantId: undefined, variants }
          : it
      )
    );
  };

  const onVariantSelect = (key: number, variantId: string, item: OrderItemDraft) => {
    const variant = item.variants.find((v) => v.id === variantId);
    const product = products.find((p) => p.id === item.productId);
    const base = product?.price ?? 0;
    setItems((prev) =>
      prev.map((it) =>
        it.key === key ? { ...it, variantId, unitPrice: String(base + (variant?.priceDelta ?? 0)) } : it
      )
    );
  };

  const updateItem = (key: number, patch: Partial<OrderItemDraft>) =>
    setItems((prev) => prev.map((it) => (it.key === key ? { ...it, ...patch } : it)));

  const removeItem = (key: number) => setItems((prev) => prev.filter((it) => it.key !== key));

  const subtotal = items.reduce((sum, it) => sum + (Number(it.unitPrice) || 0) * (Number(it.quantity) || 0), 0);
  const effectiveTaxRate = items.length > 0 && taxRate === '' && products.length > 0
    ? products.filter((p) => p.taxRate).reduce((acc, p) => acc + (p.taxRate || 0), 0) / Math.max(1, products.filter((p) => p.taxRate).length)
    : taxRate === '' ? 0 : Number(taxRate);
  const taxAmount = Math.round((subtotal * effectiveTaxRate) / 100);
  const total = subtotal + taxAmount;

  const handleCreateOrder = async () => {
    if (!user || items.length === 0) {
      toast.error(t.orders.noItems);
      return;
    }
    const payloadItems: MerchantOrderItem[] = items
      .filter((it) => it.productId)
      .map((it) => ({
        productId: it.productId,
        quantity: Number(it.quantity) || 1,
        variantId: it.variantId || undefined,
        unitPrice: Number(it.unitPrice) || 0,
      }));
    if (payloadItems.length === 0) {
      toast.error(t.orders.noItems);
      return;
    }
    setSubmitting(true);
    try {
      await orderApi.create(user.id, {
        items: payloadItems,
        customerPhone: customerPhone || undefined,
        customerName: customerName || undefined,
        taxRate: taxRate === '' ? undefined : Number(taxRate),
      });
      toast.success(t.orders.created);
      setShowOrderForm(false);
      await load();
    } catch (err) {
      console.error('Failed to create order', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const runAction = async (order: MerchantOrder, action: 'pay' | 'fulfill' | 'cancel', successKey: keyof typeof t.orders) => {
    if (!user) return;
    if (action === 'cancel' && !window.confirm(t.orders.cancelConfirm)) return;
    setSubmitting(true);
    try {
      if (action === 'pay') await orderApi.markPaid(user.id, order.id);
      if (action === 'fulfill') await orderApi.fulfill(user.id, order.id);
      if (action === 'cancel') await orderApi.cancel(user.id, order.id);
      toast.success(t.orders[successKey] as string);
      await load();
    } catch (err) {
      console.error(`Failed to ${action} order`, err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const openRefund = (order: MerchantOrder) => {
    const max = (order.total ?? 0) - (order.refundAmount ?? 0);
    setRefundFor(order);
    setRefundForm({ ...emptyRefundForm, amount: String(max) });
  };

  const submitRefund = async () => {
    if (!user || !refundFor) return;
    setSubmitting(true);
    try {
      await refundApi.create(user.id, {
        orderId: refundFor.id,
        amount: Number(refundForm.amount),
        reason: refundForm.reason || undefined,
        requireApproval: refundForm.requireApproval,
      });
      toast.success(t.refunds.created);
      setRefundFor(null);
      await load();
    } catch (err) {
      console.error('Failed to refund order', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const statusBadge = (status: string) => {
    const map: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-700',
      PAID: 'bg-blue-100 text-blue-700',
      FULFILLED: 'bg-green-100 text-green-700',
      CANCELLED: 'bg-gray-100 text-gray-600',
      PARTIALLY_REFUNDED: 'bg-orange-100 text-orange-700',
      REFUNDED: 'bg-red-100 text-red-700',
    };
    return <span className={`px-2 py-0.5 rounded text-xs font-medium ${map[status] || 'bg-gray-100 text-gray-600'}`}>{status.replace(/_/g, ' ')}</span>;
  };

  const renderItemSummary = (order: MerchantOrder) => {
    const itemsArr = Array.isArray(order.items) ? (order.items as unknown as MerchantOrderItem[]) : [];
    return itemsArr.map((it, i) => (
      <div key={i} className="text-xs text-gray-500">
        {it.productName || it.productId.slice(0, 8)} × {it.quantity}
        {it.sku ? ` (${it.sku})` : ''}
      </div>
    ));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.orders.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.orders.subtitle}</p>
        </div>
        <Button onClick={openCreate}>{t.orders.newOrder}</Button>
      </div>

      <div className="flex items-center space-x-3">
        {['', 'PENDING', 'PAID', 'FULFILLED', 'PARTIALLY_REFUNDED', 'REFUNDED', 'CANCELLED'].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${statusFilter === s ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
          >
            {s === '' ? t.common.all : s.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : orders.length === 0 ? (
        <Card><p className="text-center text-gray-500 py-10">{t.orders.noOrders}</p></Card>
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                  <th className="pb-2 pr-4">{t.orders.orderNumber}</th>
                  <th className="pb-2 pr-4">{t.orders.customer}</th>
                  <th className="pb-2 pr-4">{t.orders.itemsLabel}</th>
                  <th className="pb-2 pr-4">{t.orders.total}</th>
                  <th className="pb-2 pr-4">{t.orders.refunded}</th>
                  <th className="pb-2 pr-4">{t.orders.status}</th>
                  <th className="pb-2 pr-4">{t.orders.date}</th>
                  <th className="pb-2">{t.orders.actions}</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} className="border-b border-gray-100 align-top">
                    <td className="py-2 pr-4 font-mono text-xs text-gray-500">{order.id.slice(0, 8)}…</td>
                    <td className="py-2 pr-4">
                      <p className="font-medium text-gray-900">{order.customerName || order.customerPhone || '-'}</p>
                      <p className="text-xs text-gray-400">{order.customerPhone}</p>
                    </td>
                    <td className="py-2 pr-4">{renderItemSummary(order)}</td>
                    <td className="py-2 pr-4 font-semibold text-gray-900">{formatCurrency(order.total)}</td>
                    <td className="py-2 pr-4 text-gray-600">{order.refundAmount ? formatCurrency(order.refundAmount) : '-'}</td>
                    <td className="py-2 pr-4">{statusBadge(order.status)}</td>
                    <td className="py-2 pr-4 text-gray-500">{order.createdAt ? formatDate(order.createdAt) : '-'}</td>
                    <td className="py-2">
                      <div className="flex flex-wrap gap-2">
                        {order.status === 'PENDING' && (
                          <>
                            <Button size="sm" onClick={() => runAction(order, 'pay', 'paid')}>{t.orders.markPaid}</Button>
                            <Button size="sm" variant="secondary" onClick={() => runAction(order, 'cancel', 'cancelled')}>{t.orders.cancel}</Button>
                          </>
                        )}
                        {order.status === 'PAID' && (
                          <Button size="sm" onClick={() => runAction(order, 'fulfill', 'fulfilled')}>{t.orders.fulfill}</Button>
                        )}
                        {!['CANCELLED', 'REFUNDED'].includes(order.status) && (order.refundAmount ?? 0) < (order.total ?? 0) && (
                          <Button size="sm" variant="danger" onClick={() => openRefund(order)}>{t.orders.refund}</Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={showOrderForm} onClose={() => setShowOrderForm(false)} title={t.orders.newOrder}>
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.orders.customerPhone} value={customerPhone} onChange={(e) => setCustomerPhone(e.target.value)} />
            <Input label={t.orders.customerName} value={customerName} onChange={(e) => setCustomerName(e.target.value)} />
          </div>
          <Input label={t.orders.taxRate} type="number" min={0} value={taxRate} placeholder="0" onChange={(e) => setTaxRate(e.target.value)} />

          <div className="space-y-3">
            {items.map((it) => (
              <div key={it.key} className="border border-gray-200 rounded-lg p-3 space-y-3">
                <div className="grid grid-cols-2 gap-3">
                  <select
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    value={it.productId}
                    onChange={(e) => onProductSelect(it.key, e.target.value)}
                  >
                    <option value="">{t.orders.product}</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>{p.name}</option>
                    ))}
                  </select>
                  <Input label={t.orders.quantity} type="number" min={1} value={it.quantity} onChange={(e) => updateItem(it.key, { quantity: e.target.value })} />
                </div>
                {it.variants.length > 0 && (
                  <select
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    value={it.variantId || ''}
                    onChange={(e) => onVariantSelect(it.key, e.target.value, it)}
                  >
                    <option value="">{t.orders.variant}</option>
                    {it.variants.map((v) => (
                      <option key={v.id} value={v.id}>{v.name} ({v.sku}) +{formatCurrency(v.priceDelta)}</option>
                    ))}
                  </select>
                )}
                <div className="flex items-end justify-between">
                  <Input label={t.orders.unitPrice} type="number" min={0} value={it.unitPrice} onChange={(e) => updateItem(it.key, { unitPrice: e.target.value })} />
                  <Button size="sm" variant="ghost" onClick={() => removeItem(it.key)}>{t.orders.removeItem}</Button>
                </div>
              </div>
            ))}
          </div>

          <Button variant="secondary" onClick={addItem}>{t.orders.addItem}</Button>

          <div className="rounded-lg bg-gray-50 p-4 text-sm">
            <div className="flex justify-between text-gray-600"><span>{t.orders.subtotal}</span><span>{formatCurrency(subtotal)}</span></div>
            <div className="flex justify-between text-gray-600 mt-1"><span>{t.orders.tax}</span><span>{formatCurrency(taxAmount)}</span></div>
            <div className="flex justify-between font-bold text-gray-900 mt-2"><span>{t.orders.total}</span><span>{formatCurrency(total)}</span></div>
          </div>

          <div className="flex space-x-3">
            <Button onClick={handleCreateOrder} loading={submitting} className="flex-1">{t.common.create}</Button>
            <Button variant="secondary" onClick={() => setShowOrderForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!refundFor} onClose={() => setRefundFor(null)} title={t.orders.refund}>
        <div className="space-y-4">
          {refundFor && (
            <p className="text-sm text-gray-500">
              {t.refunds.order}: <span className="font-mono">{refundFor.id.slice(0, 8)}…</span> — {formatCurrency(refundFor.total)}
            </p>
          )}
          <Input label={t.refunds.amount} type="number" min={0} value={refundForm.amount} onChange={(e) => setRefundForm((f) => ({ ...f, amount: e.target.value }))} />
          <Input label={t.refunds.reason} value={refundForm.reason} onChange={(e) => setRefundForm((f) => ({ ...f, reason: e.target.value }))} />
          <label className="flex items-center space-x-2 text-sm text-gray-700">
            <input type="checkbox" checked={refundForm.requireApproval} onChange={(e) => setRefundForm((f) => ({ ...f, requireApproval: e.target.checked }))} className="rounded" />
            <span>{t.refunds.requireApproval}</span>
          </label>
          <div className="flex space-x-3">
            <Button onClick={submitRefund} loading={submitting} className="flex-1">{t.common.submit}</Button>
            <Button variant="secondary" onClick={() => setRefundFor(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
