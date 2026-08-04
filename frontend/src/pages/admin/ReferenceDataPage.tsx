import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useTranslation } from '../../i18n';
import { refDataApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/modals/Modal';
import { formatDate, getApiErrorMessage, cn } from '../../utils';
import type { ReferenceType, ReferenceTypeSummary, ReferenceValue } from '../../types';

function StatusBadge({ active }: { active: boolean }) {
  return (
    <span className={cn('inline-flex px-2 py-1 rounded-full text-xs font-medium',
      active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600')}>
      {active ? 'Active' : 'Inactive'}
    </span>
  );
}

interface ValueRowProps {
  value: ReferenceValue;
  onSave: (id: string, data: { value: string; code: string; sortOrder: number; active: boolean }) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
  t: ReturnType<typeof useTranslation>['t'];
}

function ValueRow({ value, onSave, onDelete, t }: ValueRowProps) {
  const [editing, setEditing] = useState(false);
  const [val, setVal] = useState(value.value);
  const [code, setCode] = useState(value.code);
  const [order, setOrder] = useState(value.sortOrder);
  const [active, setActive] = useState(value.active);
  const [saving, setSaving] = useState(false);

  const save = async () => {
    setSaving(true);
    try {
      await onSave(value.id, { value: val, code, sortOrder: order, active });
      setEditing(false);
    } catch (e) {
      toast.error(getApiErrorMessage(e, t.refData.saveFailed));
    } finally {
      setSaving(false);
    }
  };

  const del = async () => {
    if (!window.confirm(t.refData.deleteValueConfirm)) return;
    try {
      await onDelete(value.id);
    } catch (e) {
      toast.error(getApiErrorMessage(e, t.refData.deleteFailed));
    }
  };

  if (!editing) {
    return (
      <div className="flex items-center justify-between py-2 border-b border-gray-100 last:border-0">
        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-400 w-8">{value.sortOrder}</span>
          <div>
            <p className="text-sm font-medium text-gray-900">{value.value}</p>
            <p className="text-xs text-gray-400 font-mono">{value.code}</p>
          </div>
          <StatusBadge active={value.active} />
        </div>
        <div className="flex gap-2">
          <Button variant="ghost" size="sm" onClick={() => { setVal(value.value); setCode(value.code); setOrder(value.sortOrder); setActive(value.active); setEditing(true); }}>
            {t.common.edit}
          </Button>
          <Button variant="danger" size="sm" onClick={del}>{t.common.delete}</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-wrap items-end gap-2 py-2 border-b border-gray-100 last:border-0">
      <div className="w-20">
        <Input label={t.refData.sortOrder} type="number" value={String(order)} onChange={(e) => setOrder(Number(e.target.value))} />
      </div>
      <div className="flex-1 min-w-40">
        <Input label={t.refData.value} value={val} onChange={(e) => setVal(e.target.value)} />
      </div>
      <div className="flex-1 min-w-32">
        <Input label={t.refData.valueCode} value={code} onChange={(e) => setCode(e.target.value)} />
      </div>
      <label className="flex items-center gap-2 pb-2.5 text-sm text-gray-600">
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        {t.refData.active}
      </label>
      <div className="flex gap-2 pb-0.5">
        <Button size="sm" onClick={save} loading={saving}>{t.common.save}</Button>
        <Button variant="ghost" size="sm" onClick={() => setEditing(false)}>{t.common.cancel}</Button>
      </div>
    </div>
  );
}

export function ReferenceDataPage() {
  const { t } = useTranslation();
  const [types, setTypes] = useState<ReferenceTypeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeModal, setTypeModal] = useState<null | { mode: 'create' } | { mode: 'edit'; type: ReferenceTypeSummary }>(null);
  const [typeCode, setTypeCode] = useState('');
  const [typeDesc, setTypeDesc] = useState('');
  const [typeActive, setTypeActive] = useState(true);
  const [savingType, setSavingType] = useState(false);
  const [valuesType, setValuesType] = useState<ReferenceType | null>(null);
  const [valuesLoading, setValuesLoading] = useState(false);
  const [newValue, setNewValue] = useState({ value: '', code: '', sortOrder: '' });
  const [addingValue, setAddingValue] = useState(false);

  const loadTypes = async () => {
    setLoading(true);
    try {
      const data = await refDataApi.getTypes(0, 100);
      setTypes(data.content || []);
    } catch (e) {
      toast.error(getApiErrorMessage(e, t.common.error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTypes();
  }, []);

  const openCreate = () => {
    setTypeCode('');
    setTypeDesc('');
    setTypeActive(true);
    setTypeModal({ mode: 'create' });
  };

  const openEdit = (type: ReferenceTypeSummary) => {
    setTypeCode(type.code);
    setTypeDesc(type.description);
    setTypeActive(type.active);
    setTypeModal({ mode: 'edit', type });
  };

  const submitType = async () => {
    if (!typeCode.trim() || !typeDesc.trim()) return;
    setSavingType(true);
    try {
      if (typeModal?.mode === 'create') {
        await refDataApi.createType({ code: typeCode, description: typeDesc, active: typeActive });
        toast.success(t.common.success);
      } else if (typeModal?.mode === 'edit' && typeModal.type) {
        await refDataApi.updateType(typeModal.type.id, { description: typeDesc, active: typeActive });
        toast.success(t.common.success);
      }
      setTypeModal(null);
      await loadTypes();
    } catch (e) {
      toast.error(getApiErrorMessage(e, t.refData.saveFailed));
    } finally {
      setSavingType(false);
    }
  };

  const deleteType = async (type: ReferenceTypeSummary) => {
    if (!window.confirm(t.refData.deleteTypeConfirm)) return;
    try {
      await refDataApi.deleteType(type.id);
      toast.success(t.common.success);
      await loadTypes();
    } catch (e) {
      toast.error(getApiErrorMessage(e, t.refData.deleteFailed));
    }
  };

  const openValues = async (type: ReferenceTypeSummary) => {
    setValuesLoading(true);
    setValuesType(null);
    try {
      const detail = await refDataApi.getType(type.id);
      setValuesType(detail);
      setNewValue({ value: '', code: '', sortOrder: '' });
    } catch (e) {
      toast.error(getApiErrorMessage(e, t.common.error));
    } finally {
      setValuesLoading(false);
    }
  };

  const addValue = async () => {
    if (!valuesType || !newValue.value.trim() || !newValue.code.trim()) return;
    setAddingValue(true);
    try {
      const sortOrder = newValue.sortOrder ? Number(newValue.sortOrder) : (valuesType.values.length + 1);
      const added = await refDataApi.addValue(valuesType.id, { value: newValue.value, code: newValue.code, sortOrder });
      setValuesType({ ...valuesType, values: [...valuesType.values, added] });
      setNewValue({ value: '', code: '', sortOrder: '' });
    } catch (e) {
      toast.error(getApiErrorMessage(e, t.refData.saveFailed));
    } finally {
      setAddingValue(false);
    }
  };

  const saveValue = async (id: string, data: { value: string; code: string; sortOrder: number; active: boolean }) => {
    if (!valuesType) return;
    const updated = await refDataApi.updateValue(id, data);
    setValuesType({ ...valuesType, values: valuesType.values.map((v) => (v.id === id ? updated : v)) });
    toast.success(t.common.success);
  };

  const deleteValue = async (id: string) => {
    if (!valuesType) return;
    await refDataApi.deleteValue(id);
    setValuesType({ ...valuesType, values: valuesType.values.filter((v) => v.id !== id) });
    toast.success(t.common.success);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t.refData.title}</h1>
          <p className="text-sm text-gray-500 mt-1">{t.refData.subtitle}</p>
        </div>
        <Button onClick={openCreate}>{t.refData.newType}</Button>
      </div>

      <Card>
        {loading ? (
          <p className="text-center text-gray-500 py-8">{t.common.loading}</p>
        ) : types.length === 0 ? (
          <p className="text-center text-gray-500 py-8">{t.refData.noTypes}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.refData.typeCode}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.refData.description}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.refData.active}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.refData.valuesCount}</th>
                  <th className="text-left py-3 px-2 font-medium text-gray-500">{t.refData.updated}</th>
                  <th className="text-right py-3 px-2 font-medium text-gray-500">{t.common.actions}</th>
                </tr>
              </thead>
              <tbody>
                {types.map((type) => (
                  <tr key={type.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-2 font-mono font-medium text-gray-900">{type.code}</td>
                    <td className="py-3 px-2 text-gray-600">{type.description}</td>
                    <td className="py-3 px-2"><StatusBadge active={type.active} /></td>
                    <td className="py-3 px-2 text-gray-600">{type.valueCount}</td>
                    <td className="py-3 px-2 text-gray-500">{formatDate(type.updatedAt)}</td>
                    <td className="py-3 px-2 text-right">
                      <div className="flex gap-2 justify-end">
                        <Button variant="primary" size="sm" onClick={() => openValues(type)}>{t.refData.manageValues}</Button>
                        <Button variant="ghost" size="sm" onClick={() => openEdit(type)}>{t.common.edit}</Button>
                        <Button variant="danger" size="sm" onClick={() => deleteType(type)}>{t.common.delete}</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal open={typeModal !== null} onClose={() => setTypeModal(null)}
        title={typeModal?.mode === 'edit' ? t.refData.editType : t.refData.createType}>
        <div className="space-y-4">
          <Input
            label={t.refData.typeCode}
            value={typeCode}
            onChange={(e) => setTypeCode(e.target.value)}
            placeholder={t.refData.typeCodePlaceholder}
            disabled={typeModal?.mode === 'edit'}
          />
          <Input
            label={t.refData.description}
            value={typeDesc}
            onChange={(e) => setTypeDesc(e.target.value)}
            placeholder={t.refData.descriptionPlaceholder}
          />
          <label className="flex items-center gap-2 text-sm text-gray-600">
            <input type="checkbox" checked={typeActive} onChange={(e) => setTypeActive(e.target.checked)} />
            {t.refData.active}
          </label>
          <div className="flex gap-3">
            <Button onClick={submitType} loading={savingType} className="flex-1">{t.common.save}</Button>
            <Button variant="secondary" onClick={() => setTypeModal(null)} className="flex-1">{t.common.cancel}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={valuesType !== null} onClose={() => setValuesType(null)}
        title={valuesType ? `${t.refData.typeValues} ${valuesType.code}` : ''}>
        {valuesLoading ? (
          <p className="text-center text-gray-500 py-8">{t.common.loading}</p>
        ) : valuesType ? (
          <div className="space-y-4">
            <p className="text-sm text-gray-500">{valuesType.description}</p>

            {valuesType.values.length === 0 ? (
              <p className="text-center text-gray-500 py-4">{t.refData.noValues}</p>
            ) : (
              <div>
                {valuesType.values.map((v) => (
                  <ValueRow key={v.id} value={v} onSave={saveValue} onDelete={deleteValue} t={t} />
                ))}
              </div>
            )}

            <div className="border-t border-gray-100 pt-3">
              <p className="text-sm font-medium text-gray-700 mb-2">{t.refData.newValue}</p>
              <div className="flex flex-wrap items-end gap-2">
                <div className="w-20">
                  <Input label={t.refData.sortOrder} type="number" value={newValue.sortOrder}
                    onChange={(e) => setNewValue({ ...newValue, sortOrder: e.target.value })} />
                </div>
                <div className="flex-1 min-w-40">
                  <Input label={t.refData.value} value={newValue.value}
                    onChange={(e) => setNewValue({ ...newValue, value: e.target.value })} placeholder={t.refData.valuePlaceholder} />
                </div>
                <div className="flex-1 min-w-32">
                  <Input label={t.refData.valueCode} value={newValue.code}
                    onChange={(e) => setNewValue({ ...newValue, code: e.target.value })} placeholder={t.refData.valueCodePlaceholder} />
                </div>
                <Button size="sm" onClick={addValue} loading={addingValue}>{t.refData.newValue}</Button>
              </div>
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
}
