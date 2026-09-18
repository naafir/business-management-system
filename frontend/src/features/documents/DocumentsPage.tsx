import React, { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { DocumentItem, PageResponse } from '../../types';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { formatDate } from '../../lib/utils';
import { toast } from '../../hooks/useToast';
import {
  FileText,
  UploadCloud,
  Download,
  Trash2,
  Eye,
  Search,
  CheckCircle2,
  FileImage,
  FileSpreadsheet,
  File,
  ChevronLeft,
  ChevronRight,
  FolderOpen,
} from 'lucide-react';

const CATEGORIES = [
  { value: 'ALL', label: 'All Documents' },
  { value: 'EXPENSE', label: 'Expense Receipts' },
  { value: 'PURCHASE', label: 'Vendor Bills' },
  { value: 'SALE', label: 'Sales Delivery / PO' },
  { value: 'CUSTOMER', label: 'Customer KYC' },
  { value: 'SUPPLIER', label: 'Supplier Agreements' },
  { value: 'GENERAL', label: 'General / Office' },
];

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function getFileIcon(contentType: string) {
  if (contentType.includes('pdf')) return <FileText className="w-5 h-5 text-rose-500" />;
  if (contentType.includes('image')) return <FileImage className="w-5 h-5 text-emerald-500" />;
  if (contentType.includes('sheet') || contentType.includes('excel')) return <FileSpreadsheet className="w-5 h-5 text-emerald-600" />;
  return <File className="w-5 h-5 text-indigo-500" />;
}

export function DocumentsPage() {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [page, setPage] = useState(0);
  const pageSize = 12;

  // Upload modal state
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadCategory, setUploadCategory] = useState('GENERAL');
  const [uploadNotes, setUploadNotes] = useState('');
  const [isDragging, setIsDragging] = useState(false);

  // Preview state
  const [previewDoc, setPreviewDoc] = useState<DocumentItem | null>(null);

  // Delete state
  const [deleteDoc, setDeleteDoc] = useState<DocumentItem | null>(null);

  // Query documents
  const { data: pageData, isLoading } = useQuery<PageResponse<DocumentItem>>({
    queryKey: ['documents', selectedCategory, search, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (selectedCategory !== 'ALL') params.set('entityType', selectedCategory);
      if (search.trim()) params.set('search', search.trim());
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'createdAt');
      params.set('sortDir', 'desc');
      return api.get<PageResponse<DocumentItem>>(`/documents?${params.toString()}`);
    },
  });

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (!uploadFile) return;
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('entityType', uploadCategory);
      if (uploadNotes.trim()) formData.append('notes', uploadNotes.trim());

      const token = localStorage.getItem('token');
      const res = await fetch('/api/v1/documents/upload', {
        method: 'POST',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: formData,
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Failed to upload document');
      }
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      toast.success('File uploaded successfully!');
      setIsUploadOpen(false);
      setUploadFile(null);
      setUploadNotes('');
      setUploadCategory('GENERAL');
    },
    onError: (err: any) => {
      toast.error(err.message || 'Upload failed');
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/documents/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      toast.success('Document deleted');
      setDeleteDoc(null);
    },
    onError: (err: any) => {
      toast.error(err.message || 'Could not delete document');
    },
  });

  const documents = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      setUploadFile(e.dataTransfer.files[0]);
      setIsUploadOpen(true);
    }
  };

  const handleDownload = (doc: DocumentItem) => {
    window.open(`/api/v1/documents/${doc.id}/download`, '_blank');
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <FolderOpen className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Document & Attachment Storage
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Securely upload, preview, and organize receipts, vendor bills, customer orders, and tax documents.
          </p>
        </div>
        <Button onClick={() => setIsUploadOpen(true)} className="gap-2 self-start sm:self-auto">
          <UploadCloud className="w-4 h-4" /> Upload Document
        </Button>
      </div>

      {/* Filter / Search Bar */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col md:flex-row gap-3 items-center justify-between">
            <div className="relative w-full md:w-80">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <Input
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                placeholder="Search documents by name or notes..."
                className="pl-9 text-xs"
              />
            </div>

            <div className="flex flex-wrap gap-1.5 w-full md:w-auto">
              {CATEGORIES.map((cat) => (
                <button
                  key={cat.value}
                  onClick={() => {
                    setSelectedCategory(cat.value);
                    setPage(0);
                  }}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                    selectedCategory === cat.value
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                  }`}
                >
                  {cat.label}
                </button>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Drag & Drop Quick Zone */}
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        className={`border-2 border-dashed rounded-2xl p-6 text-center transition-all ${
          isDragging
            ? 'border-indigo-500 bg-indigo-50/50 dark:bg-indigo-950/20 scale-[1.01]'
            : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-slate-50/50 dark:bg-slate-900/20'
        }`}
      >
        <UploadCloud className="w-8 h-8 text-indigo-500 mx-auto mb-2" />
        <p className="text-xs font-semibold text-slate-700 dark:text-slate-200">
          Drag & drop files here to upload instantly, or{' '}
          <button
            onClick={() => fileInputRef.current?.click()}
            className="text-indigo-600 dark:text-indigo-400 hover:underline"
          >
            browse your device
          </button>
        </p>
        <p className="text-[11px] text-slate-400 mt-1">
          Supports PDF invoices, images (JPG, PNG, WEBP), and Excel spreadsheets up to 15MB.
        </p>
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          onChange={(e) => {
            if (e.target.files && e.target.files.length > 0) {
              setUploadFile(e.target.files[0]);
              setIsUploadOpen(true);
            }
          }}
        />
      </div>

      {/* Documents Grid / Table */}
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <FileText className="w-4 h-4 text-indigo-500" />
              Stored Files ({totalElements})
            </CardTitle>
          </div>
          <CardDescription className="text-xs">
            Permanent, encrypted-at-rest file repository for audit compliance.
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-12 text-center">
              <div className="animate-spin w-8 h-8 border-2 border-indigo-600 border-t-transparent rounded-full mx-auto mb-3" />
              <p className="text-xs text-slate-500">Loading documents...</p>
            </div>
          ) : documents.length === 0 ? (
            <div className="p-12 text-center">
              <FolderOpen className="w-12 h-12 text-slate-300 dark:text-slate-700 mx-auto mb-3" />
              <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">No Documents Found</p>
              <p className="text-xs text-slate-400 mt-1">Upload supplier bills, receipts, or legal documents above.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 dark:bg-slate-900/60 text-slate-500 dark:text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-200 dark:border-slate-800">
                  <tr>
                    <th className="px-5 py-3.5">Document Name</th>
                    <th className="px-4 py-3.5">Category</th>
                    <th className="px-4 py-3.5">Size</th>
                    <th className="px-4 py-3.5">Uploaded By</th>
                    <th className="px-4 py-3.5">Date</th>
                    <th className="px-4 py-3.5">Notes</th>
                    <th className="px-5 py-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {documents.map((doc) => (
                    <tr
                      key={doc.id}
                      className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors"
                    >
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <div className="p-2 rounded-lg bg-slate-100 dark:bg-slate-800 shrink-0">
                            {getFileIcon(doc.fileType)}
                          </div>
                          <div>
                            <p className="font-semibold text-slate-900 dark:text-slate-100 hover:text-indigo-600 transition-colors max-w-xs truncate" title={doc.fileName}>
                              {doc.fileName}
                            </p>
                            <span className="text-[11px] text-slate-400 uppercase font-mono">
                              {doc.fileType.split('/')[1] || 'FILE'}
                            </span>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3.5">
                        <Badge variant="default" className="text-[11px] font-medium">
                          {doc.entityType}
                        </Badge>
                      </td>
                      <td className="px-4 py-3.5 text-slate-600 dark:text-slate-300 font-mono text-[11px]">
                        {formatBytes(doc.fileSizeBytes)}
                      </td>
                      <td className="px-4 py-3.5 text-slate-600 dark:text-slate-400">
                        {doc.uploadedByName || 'System'}
                      </td>
                      <td className="px-4 py-3.5 text-slate-500 whitespace-nowrap">
                        {formatDate(doc.createdAt)}
                      </td>
                      <td className="px-4 py-3.5 text-slate-500 max-w-xs truncate" title={doc.notes || ''}>
                        {doc.notes || '—'}
                      </td>
                      <td className="px-5 py-3.5 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => setPreviewDoc(doc)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                            title="Preview file"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDownload(doc)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/40 transition-colors"
                            title="Download file"
                          >
                            <Download className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => setDeleteDoc(doc)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors"
                            title="Delete file"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-5 py-3 border-t border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
              <span className="text-xs text-slate-500">
                Page {page + 1} of {totalPages} ({totalElements} files)
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
        </CardContent>
      </Card>

      {/* Upload Modal */}
      <Modal
        isOpen={isUploadOpen}
        onClose={() => {
          setIsUploadOpen(false);
          setUploadFile(null);
        }}
        title="Upload Document"
        description="Upload a receipt, bill, or legal agreement to secure storage"
        className="max-w-md"
      >
        <div className="space-y-4">
          <div>
            <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 block mb-1.5">
              Select File *
            </label>
            <input
              type="file"
              onChange={(e) => {
                if (e.target.files && e.target.files.length > 0) {
                  setUploadFile(e.target.files[0]);
                }
              }}
              className="w-full text-xs text-slate-500 file:mr-3 file:py-2 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100 dark:file:bg-indigo-950/50 dark:file:text-indigo-300"
            />
            {uploadFile && (
              <p className="text-[11px] text-emerald-600 dark:text-emerald-400 mt-1 flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" /> Selected: {uploadFile.name} ({formatBytes(uploadFile.size)})
              </p>
            )}
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 block mb-1.5">
              Document Category *
            </label>
            <select
              value={uploadCategory}
              onChange={(e) => setUploadCategory(e.target.value)}
              className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-3 py-2 text-xs text-slate-900 dark:text-white"
            >
              {CATEGORIES.filter((c) => c.value !== 'ALL').map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 block mb-1.5">
              Notes / Tags (Optional)
            </label>
            <Input
              value={uploadNotes}
              onChange={(e) => setUploadNotes(e.target.value)}
              placeholder="e.g. Invoice INV-2026-0012, March Office Rent"
              className="text-xs"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100 dark:border-slate-800">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setIsUploadOpen(false);
                setUploadFile(null);
              }}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={() => uploadMutation.mutate()}
              disabled={!uploadFile || uploadMutation.isPending}
            >
              {uploadMutation.isPending ? 'Uploading...' : 'Upload File'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Preview Modal */}
      {previewDoc && (
        <Modal
          isOpen={true}
          onClose={() => setPreviewDoc(null)}
          title={previewDoc.fileName}
          description={`${previewDoc.entityType} • ${formatBytes(previewDoc.fileSizeBytes)}`}
          className="max-w-3xl"
        >
          <div className="space-y-4">
            <div className="rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden bg-slate-950/5 min-h-[300px] max-h-[500px] flex items-center justify-center">
              {previewDoc.fileType.startsWith('image/') ? (
                <img
                  src={`/api/v1/documents/${previewDoc.id}/preview`}
                  alt={previewDoc.fileName}
                  className="max-h-[480px] max-w-full object-contain mx-auto"
                />
              ) : previewDoc.fileType.includes('pdf') ? (
                <iframe
                  src={`/api/v1/documents/${previewDoc.id}/preview`}
                  title={previewDoc.fileName}
                  className="w-full h-[450px] border-0"
                />
              ) : (
                <div className="text-center p-8">
                  <File className="w-12 h-12 text-slate-400 mx-auto mb-2" />
                  <p className="text-xs text-slate-600 dark:text-slate-400">
                    Preview not directly embeddable for this file type.
                  </p>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleDownload(previewDoc)}
                    className="mt-3 gap-1.5"
                  >
                    <Download className="w-3.5 h-3.5" /> Download to View
                  </Button>
                </div>
              )}
            </div>

            <div className="flex justify-between items-center pt-2">
              <span className="text-[11px] text-slate-400">
                Uploaded {formatDate(previewDoc.createdAt)} by {previewDoc.uploadedByName || 'System'}
              </span>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => handleDownload(previewDoc)} className="gap-1.5">
                  <Download className="w-3.5 h-3.5" /> Download
                </Button>
                <Button size="sm" onClick={() => setPreviewDoc(null)}>
                  Close
                </Button>
              </div>
            </div>
          </div>
        </Modal>
      )}

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={!!deleteDoc}
        onClose={() => setDeleteDoc(null)}
        onConfirm={() => {
          if (deleteDoc) deleteMutation.mutate(deleteDoc.id);
        }}
        title="Delete Document"
        message={`Are you sure you want to permanently delete "${deleteDoc?.fileName}"? This action cannot be undone.`}
        confirmLabel="Delete File"
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
export default DocumentsPage;
