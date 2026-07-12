import React, { useState, useEffect } from 'react';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { Modal } from '../components/Modal';
import { api } from '../services/api';
import { Plus, Edit2, Trash2, Search } from 'lucide-react';

export const DocsManager: React.FC = () => {
  const [docs, setDocs] = useState<any[]>([]);
  const [search, setSearch] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingDoc, setEditingDoc] = useState<any | null>(null);

  const fetchDocs = async () => {
    try {
      const data = await api.getVideos();
      setDocs(data || []);
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchDocs();
  }, []);

  const filteredDocs = docs.filter(d => d.title?.toLowerCase().includes(search.toLowerCase()));

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to delete this resource?')) {
      try {
        await api.deleteVideo(id);
        fetchDocs();
      } catch (error) {
        alert('Failed to delete resource');
      }
    }
  };

  const openModal = (doc?: any) => {
    setEditingDoc(doc || null);
    setIsModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingDoc?.id) {
        await api.updateVideo(editingDoc.id, editingDoc);
      } else if (editingDoc) {
        await api.createVideo(editingDoc);
      }
      fetchDocs();
      setIsModalOpen(false);
    } catch (error) {
      alert('Failed to save resource');
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-lg">
        <h1>Resources Management (Videos)</h1>
        <Button onClick={() => openModal()}><Plus size={18} /> Add Resource</Button>
      </div>

      <Card>
        <div className="flex gap-md mb-md items-center">
          <div style={{ position: 'relative', width: '300px' }}>
            <Search size={18} style={{ position: 'absolute', left: '12px', top: '14px', color: '#94A3B8' }} />
            <Input 
              placeholder="Search resources..." 
              value={search} 
              onChange={e => setSearch(e.target.value)}
              style={{ paddingLeft: '40px' }}
            />
          </div>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>URL</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredDocs.map(doc => (
                <tr key={doc.id}>
                  <td>{doc.title}</td>
                  <td>{doc.youtubeUrl || doc.url}</td>
                  <td>
                    <span className="badge badge-success">Active</span>
                  </td>
                  <td>
                    <div className="flex gap-sm">
                      <Button variant="ghost" onClick={() => openModal(doc)} style={{ padding: '8px' }}><Edit2 size={16} /></Button>
                      <Button variant="ghost" onClick={() => handleDelete(doc.id)} style={{ padding: '8px', color: 'var(--color-destructive)' }}><Trash2 size={16} /></Button>
                    </div>
                  </td>
                </tr>
              ))}
              {filteredDocs.length === 0 && (
                <tr>
                  <td colSpan={4} style={{ textAlign: 'center', padding: '32px' }}>No resources found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingDoc?.id ? "Edit Resource" : "Add Resource"}>
        <form onSubmit={handleSave} className="flex-col gap-md">
          <Input 
            label="Title" 
            value={editingDoc?.title || ''} 
            onChange={e => setEditingDoc((prev: any) => ({ ...prev, title: e.target.value }))} 
            required
          />
          <Input 
            label="YouTube URL" 
            value={editingDoc?.youtubeUrl || ''} 
            onChange={e => setEditingDoc((prev: any) => ({ ...prev, youtubeUrl: e.target.value }))} 
            required
            placeholder="https://youtube.com/..."
          />
          <div className="flex justify-end gap-sm mt-md">
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button type="submit">Save</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
