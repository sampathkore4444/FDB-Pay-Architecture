import { useEffect, useState } from 'react';
import { useTranslation } from '../../i18n';
import { directoryApi } from '../../services/api';
import { Card } from '../../components/cards/Card';
import { Input } from '../../components/ui/Input';

interface MerchantResult {
  id: string;
  businessName: string;
  category: string;
  distance?: number;
  rating?: number;
  address?: string;
  qrStaticUrl?: string;
}

const categoryIcons: Record<string, string> = {
  food: '🍜',
  transport: '🚗',
  shopping: '🛍️',
  services: '🔧',
  health: '🏥',
  education: '📚',
  other: '🏪',
};

export function MerchantDirectoryPage() {
  const { t } = useTranslation();
  const [merchants, setMerchants] = useState<MerchantResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [selectedMerchant, setSelectedMerchant] = useState<MerchantResult | null>(null);
  const [coords, setCoords] = useState<{ latitude: number; longitude: number } | null>(null);

  const loadMerchants = async () => {
    setLoading(true);
    try {
      let data: MerchantResult[];
      if (search) {
        data = await directoryApi.searchMerchants(search, category || undefined);
      } else {
        data = await directoryApi.getNearbyMerchants(category || undefined, coords || undefined);
      }
      setMerchants(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => setCoords({ latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
        () => setCoords(null),
        { timeout: 5000 }
      );
    }
  }, []);

  useEffect(() => {
    loadMerchants();
  }, [category, coords]);

  const handleSearch = () => {
    loadMerchants();
  };

  const uniqueCategories = [...new Set(merchants.map((m) => m.category).filter(Boolean))];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t.directory.title}</h1>

      <div className="flex flex-col md:flex-row gap-3">
        <div className="flex-1">
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t.directory.searchPlaceholder}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
        </div>
        <div className="w-full md:w-48">
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg"
          >
            <option value="">{t.directory.allCategories}</option>
            {uniqueCategories.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8">{t.common.loading}</div>
      ) : merchants.length === 0 ? (
        <Card>
          <p className="text-center text-gray-500 py-8">{t.directory.noResults}</p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {merchants.map((m) => (
            <div
              key={m.id}
              className="bg-white border border-gray-200 rounded-xl p-4 hover:shadow-md transition-shadow cursor-pointer"
              onClick={() => setSelectedMerchant(m)}
            >
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  <h3 className="font-semibold text-gray-900">{m.businessName}</h3>
                  <p className="text-sm text-gray-500">
                    {categoryIcons[m.category] || '🏪'} {m.category}
                  </p>
                  {m.distance !== undefined && (
                    <p className="text-xs text-gray-400">
                      📍 {m.distance.toFixed(1)} {t.directory.km}
                    </p>
                  )}
                </div>
                {m.rating !== undefined && (
                  <div className="flex items-center space-x-1">
                    <span className="text-yellow-500">★</span>
                    <span className="text-sm font-medium text-gray-700">{m.rating.toFixed(1)}</span>
                  </div>
                )}
              </div>
              {m.address && (
                <p className="text-xs text-gray-400 mt-2">{m.address}</p>
              )}
            </div>
          ))}
        </div>
      )}

      {selectedMerchant && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="fixed inset-0 bg-black/50" onClick={() => setSelectedMerchant(null)} />
          <div className="relative bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">{selectedMerchant.businessName}</h3>
              <button onClick={() => setSelectedMerchant(null)} className="text-gray-400 hover:text-gray-600">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="space-y-3 text-sm">
              <p><span className="font-medium">{t.common.category}:</span> {selectedMerchant.category}</p>
              {selectedMerchant.address && <p><span className="font-medium">{t.merchant.address}:</span> {selectedMerchant.address}</p>}
              {selectedMerchant.distance !== undefined && (
                <p><span className="font-medium">{t.directory.distance}:</span> {selectedMerchant.distance.toFixed(1)} {t.directory.km}</p>
              )}
              {selectedMerchant.rating !== undefined && (
                <p><span className="font-medium">{t.directory.rating}:</span> ★ {selectedMerchant.rating.toFixed(1)}</p>
              )}
            </div>
            {selectedMerchant.qrStaticUrl && (
              <div className="mt-4 text-center">
                <img src={selectedMerchant.qrStaticUrl} alt="QR" className="w-48 h-48 mx-auto rounded-lg" />
                <p className="text-xs text-gray-400 mt-2">Scan to pay</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
