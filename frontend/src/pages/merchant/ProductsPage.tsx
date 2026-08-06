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
import type { Product } from '../../types';

const emptyForm = { name: '', price: '', category: '', description: '', imageUrl: '' };

export function ProductsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      setProducts(await catalogApi.list(user.id));
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

  const set = (key: keyof typeof emptyForm, value: string) => setForm((f) => ({ ...f, [key]: value }));

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
      ) : products.length === 0 ? (
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
              {product.description && <p className="text-sm text-gray-500 mt-1 line-clamp-2">{product.description}</p>}
              <div className="mt-4 flex justify-end space-x-2">
                <Button size="sm" variant="ghost" onClick={() => openEdit(product)}>{t.common.edit}</Button>
                <Button size="sm" variant="danger" onClick={() => remove(product)}>{t.common.delete}</Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={() => setShowForm(false)} title={editing ? t.products.editProduct : t.products.addProduct}>
        <div className="space-y-4">
          <Input label={t.products.name} value={form.name} onChange={(e) => set('name', e.target.value)} />
          <div className="grid grid-cols-2 gap-4">
            <Input label={t.products.price} type="number" value={form.price} onChange={(e) => set('price', e.target.value)} />
            <Input label={t.products.category} value={form.category} onChange={(e) => set('category', e.target.value)} />
          </div>
          <Input label={t.products.imageUrl} value={form.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} />
          <Input label={t.products.description} value={form.description} onChange={(e) => set('description', e.target.value)} />
          <div className="flex space-x-3">
            <Button onClick={handleSave} loading={submitting} disabled={!form.name || !form.price} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
