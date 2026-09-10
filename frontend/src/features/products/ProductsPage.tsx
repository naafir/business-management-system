import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Product, ProductCategory, PageResponse } from '../../types';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { ProductModal } from './ProductModal';
import { CategoryModal } from './CategoryModal';
import { formatCurrency } from '../../lib/utils';
import {
  Package,
  Plus,
  Search,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Edit2,
  FolderPlus,
  Boxes,
  TrendingUp,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';

export function ProductsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [activeFilter, setActiveFilter] = useState<'all' | 'active' | 'inactive'>('all');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [isProductModalOpen, setIsProductModalOpen] = useState(false);
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

  // Fetch categories
  const { data: categories = [] } = useQuery<ProductCategory[]>({
    queryKey: ['categories'],
    queryFn: () => api.get<ProductCategory[]>('/categories'),
  });

  // Fetch products
  const { data: pageData, isLoading } = useQuery<PageResponse<Product>>({
    queryKey: ['products', search, selectedCategory, activeFilter, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (search.trim()) params.set('search', search.trim());
      if (selectedCategory) params.set('categoryId', selectedCategory);
      if (activeFilter === 'active') params.set('activeOnly', 'true');
      if (activeFilter === 'inactive') params.set('activeOnly', 'false');
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'name');
      params.set('sortDir', 'asc');
      return api.get<PageResponse<Product>>(`/products?${params.toString()}`);
    },
  });

  // Toggle status mutation
  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      api.patch<Product>(`/products/${id}/status?active=${active}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
  });

  const products = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  // Stats calculation
  const isProductLowStock = (p: Product) => Boolean(p.isLowStock ?? p.lowStock ?? (p.currentStock <= p.minStockLevel));
  const lowStockCount = products.filter(isProductLowStock).length;
  const totalValuation = products.reduce((acc, p) => acc + p.currentStock * p.purchasePrice, 0);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <Package className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Product Catalog
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Manage your master inventory items, SKU codes, HSN/SAC numbers, pricing, and GST rates.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            onClick={() => setIsCategoryModalOpen(true)}
            className="flex items-center gap-1.5"
          >
            <FolderPlus className="w-4 h-4" />
            Add Category
          </Button>
          <Button
            onClick={() => {
              setSelectedProduct(null);
              setIsProductModalOpen(true);
            }}
            className="flex items-center gap-1.5 shadow-md shadow-indigo-600/20"
          >
            <Plus className="w-4 h-4" />
            Add Product
          </Button>
        </div>
      </div>

      {/* KPI Stats Overview */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-indigo-50/50 to-white dark:from-indigo-950/20 dark:to-slate-900 border-indigo-100 dark:border-indigo-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Products
              </p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{totalElements}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-indigo-600/10 text-indigo-600 dark:text-indigo-400">
              <Package className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-emerald-50/50 to-white dark:from-emerald-950/20 dark:to-slate-900 border-emerald-100 dark:border-emerald-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Active Catalog
              </p>
              <h3 className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                {products.filter((p) => p.active).length}
              </h3>
            </div>
            <div className="p-2.5 rounded-xl bg-emerald-600/10 text-emerald-600 dark:text-emerald-400">
              <CheckCircle2 className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-amber-50/50 to-white dark:from-amber-950/20 dark:to-slate-900 border-amber-100 dark:border-amber-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Low Stock Alerts
              </p>
              <h3 className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">{lowStockCount}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-amber-600/10 text-amber-600 dark:text-amber-400">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-blue-50/50 to-white dark:from-blue-950/20 dark:to-slate-900 border-blue-100 dark:border-blue-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Est. Page Stock Value
              </p>
              <h3 className="text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">
                {formatCurrency(totalValuation)}
              </h3>
            </div>
            <div className="p-2.5 rounded-xl bg-blue-600/10 text-blue-600 dark:text-blue-400">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by SKU, product name, brand..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full pl-10 pr-3.5 py-2 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto">
            <select
              value={selectedCategory}
              onChange={(e) => {
                setSelectedCategory(e.target.value);
                setPage(0);
              }}
              className="px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
            >
              <option value="">All Categories</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>

            <select
              value={activeFilter}
              onChange={(e) => {
                setActiveFilter(e.target.value as any);
                setPage(0);
              }}
              className="px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
            >
              <option value="all">All Status</option>
              <option value="active">Active Only</option>
              <option value="inactive">Inactive Only</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Product Data Table */}
      <Card className="overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center p-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          </div>
        ) : products.length === 0 ? (
          <div className="text-center py-12">
            <Boxes className="w-12 h-12 text-slate-400 mx-auto mb-3 stroke-[1.5]" />
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">No products found</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
              Get started by adding your first product to the master catalog.
            </p>
            <div className="mt-4">
              <Button
                onClick={() => {
                  setSelectedProduct(null);
                  setIsProductModalOpen(true);
                }}
                className="gap-1.5"
              >
                <Plus className="w-4 h-4" /> Add Product
              </Button>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-900/60 text-slate-500 dark:text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-200 dark:border-slate-800">
                <tr>
                  <th className="px-5 py-3.5">Product & SKU</th>
                  <th className="px-4 py-3.5">Category</th>
                  <th className="px-4 py-3.5">HSN/SAC</th>
                  <th className="px-4 py-3.5">Unit</th>
                  <th className="px-4 py-3.5">Buy Price</th>
                  <th className="px-4 py-3.5">Sell Price</th>
                  <th className="px-4 py-3.5">GST Rate</th>
                  <th className="px-4 py-3.5">Stock</th>
                  <th className="px-4 py-3.5">Status</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {products.map((product) => (
                  <tr
                    key={product.id}
                    className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors"
                  >
                    <td className="px-5 py-3.5">
                      <div className="font-semibold text-slate-900 dark:text-slate-100">
                        {product.name}
                      </div>
                      <div className="flex items-center gap-1.5 mt-0.5">
                        <span className="font-mono text-[11px] px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                          {product.sku}
                        </span>
                        {product.brand && (
                          <span className="text-[11px] text-slate-400">· {product.brand}</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3.5 text-slate-600 dark:text-slate-300">
                      {product.categoryName || '—'}
                    </td>
                    <td className="px-4 py-3.5 font-mono text-[11px] text-slate-600 dark:text-slate-400">
                      {product.hsnSac}
                    </td>
                    <td className="px-4 py-3.5 text-slate-600 dark:text-slate-400">{product.unit}</td>
                    <td className="px-4 py-3.5 text-slate-600 dark:text-slate-300">
                      {formatCurrency(product.purchasePrice)}
                    </td>
                    <td className="px-4 py-3.5 font-semibold text-slate-900 dark:text-slate-100">
                      {formatCurrency(product.sellingPrice)}
                    </td>
                    <td className="px-4 py-3.5">
                      {product.gstRatePercent !== undefined ? (
                        <Badge variant="info">{product.gstRatePercent}%</Badge>
                      ) : (
                        <span className="text-slate-400">Exempt</span>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="flex items-center gap-1.5">
                        <span
                          className={`font-semibold ${
                            isProductLowStock(product)
                              ? 'text-amber-600 dark:text-amber-400'
                              : 'text-slate-900 dark:text-slate-100'
                          }`}
                        >
                          {product.currentStock}
                        </span>
                        {isProductLowStock(product) && (
                          <span className="inline-flex items-center gap-0.5 text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded-full bg-amber-50 dark:bg-amber-950/50 text-amber-700 dark:text-amber-400 border border-amber-200 dark:border-amber-800">
                            <AlertTriangle className="w-2.5 h-2.5" /> Low
                          </span>
                        )}
                      </div>
                      <span className="text-[10px] text-slate-400">Min: {product.minStockLevel}</span>
                    </td>
                    <td className="px-4 py-3.5">
                      {product.active ? (
                        <Badge variant="success">Active</Badge>
                      ) : (
                        <Badge variant="default">Inactive</Badge>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => {
                            setSelectedProduct(product);
                            setIsProductModalOpen(true);
                          }}
                          className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                          title="Edit product"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() =>
                            toggleStatusMutation.mutate({
                              id: product.id,
                              active: !product.active,
                            })
                          }
                          className={`p-1.5 rounded-lg transition-colors ${
                            product.active
                              ? 'text-slate-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40'
                              : 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/40'
                          }`}
                          title={product.active ? 'Deactivate product' : 'Activate product'}
                        >
                          {product.active ? (
                            <XCircle className="w-4 h-4" />
                          ) : (
                            <CheckCircle2 className="w-4 h-4" />
                          )}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
            <span className="text-xs text-slate-500 dark:text-slate-400">
              Showing page {page + 1} of {totalPages} ({totalElements} total items)
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="gap-1 text-xs"
              >
                <ChevronLeft className="w-3.5 h-3.5" /> Prev
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="gap-1 text-xs"
              >
                Next <ChevronRight className="w-3.5 h-3.5" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Modals */}
      <ProductModal
        isOpen={isProductModalOpen}
        onClose={() => setIsProductModalOpen(false)}
        product={selectedProduct}
      />

      <CategoryModal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
      />
    </div>
  );
}
