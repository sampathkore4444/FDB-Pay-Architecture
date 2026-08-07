import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { useAuthStore } from '../../store/authStore';
import { catalogApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatCurrency } from '../../utils';
import type { Product, ProductVariant } from '../../types';

const emptyForm = { name: '', price: '', category: '', description: '', imageUrl: '', quantity: '', lowStockThreshold: '', taxRate: '', deliverable: false, deliveryContent: '' };
const emptyVariant = { sku: '', name: '', priceDelta: '', quantity: '' };

export function ProductsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [products, setProducts] = useState<Product[]>([]);
  const [lowStock, setLowStock] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [variantsFor, setVariantsFor] = useState<Product | null>(null);
  const [variants, setVariants] = useState<ProductVariant[]>([]);
  const [showVariantForm, setShowVariantForm] = useState(false);
  const [editingVariant, setEditingVariant] = useState<ProductVariant | null>(null);
  const [variantForm, setVariantForm] = useState(emptyVariant);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setProducts(await catalogApi.list(user.id));
      setLowStock(await catalogApi.lowStock(user.id));
    } catch (err) {
      console.error('Failed to load products', err);
      toast.error(t.common.loadFailed);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user]);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEdit = (product: Product) => {
    setEditing(product);
    setForm({
      name: product.name,
      price: String(product.price),
      category: product.category || '',
      description: product.description || '',
      imageUrl: product.imageUrl || '',
      quantity: product.quantity != null ? String(product.quantity) : '',
      lowStockThreshold: product.lowStockThreshold != null ? String(product.lowStockThreshold) : '',
      taxRate: product.taxRate != null ? String(product.taxRate) : '',
      deliverable: product.deliverable ?? false,
      deliveryContent: product.deliveryContent || '',
    });
    setShowForm(true);
  };

  const handleSave = async () => {
    if (!user || !form.name || !form.price) return;
    setSubmitting(true);
    try {
      const payload = {
        name: form.name,
        price: Number(form.price),
        category: form.category || undefined,
        description: form.description || undefined,
        imageUrl: form.imageUrl || undefined,
        quantity: form.quantity ? Number(form.quantity) : undefined,
        lowStockThreshold: form.lowStockThreshold ? Number(form.lowStockThreshold) : undefined,
        taxRate: form.taxRate ? Number(form.taxRate) : undefined,
        deliverable: form.deliverable || undefined,
        deliveryContent: form.deliverable && form.deliveryContent ? form.deliveryContent : undefined,
      };
      if (editing) {
        await catalogApi.update(user.id, editing.id, payload);
        toast.success(t.products.updated);
      } else {
        await catalogApi.create(user.id, payload);
        toast.success(t.products.created);
      }
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Failed to save product', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async (product: Product) => {
    if (!user || !window.confirm(t.products.deleteConfirm)) return;
    try {
      await catalogApi.delete(user.id, product.id);
      toast.success(t.common.deleted);
      await load();
    } catch (err) {
      console.error('Failed to delete product', err);
      toast.error(t.common.loadFailed);
    }
  };

  const openVariants = async (product: Product) => {
    if (!user) return;
    setVariantsFor(product);
    setVariants(await catalogApi.listVariants(user.id, product.id).catch(() => []));
  };

  const openVariantCreate = () => {
    setEditingVariant(null);
    setVariantForm(emptyVariant);
    setShowVariantForm(true);
  };

  const openVariantEdit = (variant: ProductVariant) => {
    setEditingVariant(variant);
    setVariantForm({
      sku: variant.sku,
      name: variant.name,
      priceDelta: String(variant.priceDelta),
      quantity: String(variant.quantity),
    });
    setShowVariantForm(true);
  };

  const saveVariant = async () => {
    if (!user || !variantsFor || !variantForm.sku) return;
    setSubmitting(true);
    try {
      if (editingVariant) {
        await catalogApi.updateVariant(user.id, variantsFor.id, editingVariant.id, {
          sku: variantForm.sku,
          name: variantForm.name || undefined,
          priceDelta: variantForm.priceDelta ? Number(variantForm.priceDelta) : undefined,
          quantity: variantForm.quantity ? Number(variantForm.quantity) : undefined,
        });
        toast.success(t.products.variantUpdated);
      } else {
        await catalogApi.addVariant(user.id, variantsFor.id, {
          sku: variantForm.sku,
          name: variantForm.name,
          priceDelta: variantForm.priceDelta ? Number(variantForm.priceDelta) : undefined,
          quantity: variantForm.quantity ? Number(variantForm.quantity) : undefined,
        });
        toast.success(t.products.variantCreated);
      }
      setShowVariantForm(false);
      setVariants(await catalogApi.listVariants(user.id, variantsFor.id));
    } catch (err) {
      console.error('Failed to save variant', err);
      toast.error(t.common.loadFailed);
    } finally {
      setSubmitting(false);
    }
  };

  const removeVariant = async (variant: ProductVariant) => {
    if (!user || !variantsFor || !window.confirm(t.products.deleteVariantConfirm)) return;
    try {
      await catalogApi.deleteVariant(user.id, variantsFor.id, variant.id);
      toast.success(t.products.variantDeleted);
      setVariants(await catalogApi.listVariants(user.id, variantsFor.id));
    } catch (err) {
      console.error('Failed to delete variant', err);
      toast.error(t.common.loadFailed);
    }
  };

  const set = (key: keyof typeof emptyForm, value: string | boolean) => setForm((f) => ({ ...f, [key]: value }));
  const setVariant = (key: keyof typeof emptyVariant, value: string) => setVariantForm((f) => ({ ...f, [key]: value }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.products.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.products.subtitle}</p>
        </div>
        <Button onClick={openCreate}>{t.products.addProduct}</Button>
      </div>

      {loading ? (
        <Card><p className="text-center text-gray-500 py-10">{t.common.loading}</p></Card>
      ) : (
        <>
          <Card title={t.products.lowStockTitle}>
            {lowStock.length === 0 ? (
              <p className="text-center text-gray-500 py-6">{t.products.noLowStock}</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                      <th className="pb-2 pr-4">{t.products.name}</th>
                      <th className="pb-2 pr-4">{t.products.quantity}</th>
                      <th className="pb-2 pr-4">{t.products.lowStockThreshold}</th>
                      <th className="pb-2">{t.common.status}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lowStock.map((product) => (
                      <tr key={product.id} className="border-b border-gray-100">
                        <td className="py-2 pr-4 font-medium text-gray-900">{product.name}</td>
                        <td className="py-2 pr-4 text-gray-900">{product.quantity ?? 0}</td>
                        <td className="py-2 pr-4 text-gray-600">{product.lowStockThreshold ?? 0}</td>
                        <td className="py-2"><span className="px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700">{t.products.lowStock}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          {products.length === 0 ? (
            <Card><p className="text-center text-gray-500 py-10">{t.products.noProducts}</p></Card>
          ) : (
          <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-4">
            {products.map((product) => (
              <Card key={product.id}>
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-semibold text-gray-900">{product.name}</h3>
                    {product.category && <p className="text-sm text-gray-400 mt-1">{product.category}</p>}
                  </div>
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${product.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>{product.status}</span>
                </div>
                {product.imageUrl && <img src={product.imageUrl} alt={product.name} className="mt-3 h-28 w-full object-cover rounded-lg" />}
                <p className="mt-3 text-lg font-bold text-gray-900">{formatCurrency(product.price)}</p>
                <div className="mt-1 flex flex-wrap gap-2">
                  {product.taxRate != null && <span className="px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-700">{product.taxRate}% tax</span>}
                  {product.deliverable && <span className="px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-700">{t.products.deliverable}</span>}
                </div>
                {product.quantity != null && <p className="text-sm text-gray-500 mt-1">{t.products.quantity}: {product.quantity}</p>}
                {product.description && <p className="text-sm text-gray-500 mt-1 line-clamp-2">{product.description}</p>}
                <div className="mt-4 flex justify-end space-x-2">
                  <Button size="sm" variant="ghost" onClick={() => openVariants(product)}>{t.products.variants}</Button>
                  <Button size="sm" variant="ghost" onClick={() => openEdit(product)}>{t.common.edit}</Button>
                  <Button size="sm" variant="danger" onClick={() => remove(product)}>{t.common.delete}</Button>
                </div>
              </Card>
            ))}
          </div>
          )}
        </>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={editing ? t.products.editProduct : t.products.addProduct}>
        <div className="space-y-4">
          <Input label={t.products.name} value={form.name} onChange={(e) => set('name', e.target.value)} />
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.products.price} type="number" value={form.price} onChange={(e) => set('price', e.target.value)} />
            <Input label={t.products.category} value={form.category} onChange={(e) => set('category', e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.products.quantity} type="number" min={0} value={form.quantity} onChange={(e) => set('quantity', e.target.value)} />
            <Input label={t.products.lowStockThreshold} type="number" min={0} value={form.lowStockThreshold} onChange={(e) => set('lowStockThreshold', e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.products.taxRate} type="number" min={0} value={form.taxRate} onChange={(e) => set('taxRate', e.target.value)} />
            <label className="flex items-end pb-2 text-sm text-gray-700">
              <input type="checkbox" checked={form.deliverable} onChange={(e) => set('deliverable', e.target.checked)} className="rounded mr-2" />
              <span>{t.products.deliverable}</span>
            </label>
          </div>
          {form.deliverable && <Input label={t.products.deliveryContent} value={form.deliveryContent} onChange={(e) => set('deliveryContent', e.target.value)} />}
          <Input label={t.products.imageUrl} value={form.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} />
          <Input label={t.products.description} value={form.description} onChange={(e) => set('description', e.target.value)} />
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.name || !form.price} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!variantsFor} onClose={() => setVariantsFor(null)} title={`${t.products.variants} — ${variantsFor?.name || ''}`}>
        <div className="space-y-4">
          <div className="flex justify-end">
            <Button size="sm" onClick={openVariantCreate}>{t.products.addVariant}</Button>
          </div>
          {variants.length === 0 ? (
            <p className="text-center text-gray-500 py-6">{t.products.noVariants}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs uppercase text-gray-400 border-b border-gray-200">
                    <th className="pb-2 pr-4">{t.products.sku}</th>
                    <th className="pb-2 pr-4">{t.products.name}</th>
                    <th className="pb-2 pr-4">{t.products.priceDelta}</th>
                    <th className="pb-2 pr-4">{t.products.quantity}</th>
                    <th className="pb-2">{t.common.actions}</th>
                  </tr>
                </thead>
                <tbody>
                  {variants.map((variant) => (
                    <tr key={variant.id} className="border-b border-gray-100">
                      <td className="py-2 pr-4 font-mono text-xs font-semibold text-gray-700">{variant.sku}</td>
                      <td className="py-2 pr-4 text-gray-900">{variant.name}</td>
                      <td className="py-2 pr-4 text-gray-600">{formatCurrency(variant.priceDelta)}</td>
                      <td className="py-2 pr-4 text-gray-600">{variant.quantity}</td>
                      <td className="py-2">
                        <div className="flex space-x-2">
                          <Button size="sm" variant="ghost" onClick={() => openVariantEdit(variant)}>{t.common.edit}</Button>
                          <Button size="sm" variant="danger" onClick={() => removeVariant(variant)}>{t.common.delete}</Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </Modal>

      <Modal open={showVariantForm} onClose={() => setShowVariantForm(false)} title={editingVariant ? t.common.edit : t.products.addVariant}>
        <div className="space-y-4">
          <Input label={t.products.sku} value={variantForm.sku} onChange={(e) => setVariant('sku', e.target.value)} />
          <Input label={t.products.variantName} value={variantForm.name} onChange={(e) => setVariant('name', e.target.value)} />
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.products.priceDelta} type="number" value={variantForm.priceDelta} onChange={(e) => setVariant('priceDelta', e.target.value)} />
            <Input label={t.products.quantity} type="number" value={variantForm.quantity} onChange={(e) => setVariant('quantity', e.target.value)} />
          </div>
          <div className="flex space-x-3">
            <Button onClick={saveVariant} loading={submitting} disabled={!variantForm.sku} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowVariantForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
