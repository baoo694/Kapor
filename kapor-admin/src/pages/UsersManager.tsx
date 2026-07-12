import React, { useState, useEffect } from 'react';
import { Card } from '../components/Card';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { Modal } from '../components/Modal';
import { api } from '../services/api';
import { Plus, Edit2, Trash2, Search } from 'lucide-react';

export const UsersManager: React.FC = () => {
  const [users, setUsers] = useState<any[]>([]);
  const [search, setSearch] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<any | null>(null);

  const fetchUsers = async () => {
    try {
      const data = await api.getUsers(1, search);
      setUsers(data?.content || data?.items || data || []);
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [search]);

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to deactivate this user?')) {
      try {
        await api.deleteUser(id);
        fetchUsers();
      } catch (error) {
        alert('Failed to delete user');
      }
    }
  };

  const openModal = (user?: any) => {
    setEditingUser(user || null);
    setIsModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingUser?.id) {
        await api.updateUserRole(editingUser.id, editingUser.role);
      }
      fetchUsers();
      setIsModalOpen(false);
    } catch (error) {
      alert('Failed to update user');
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-lg">
        <h1>Users Management</h1>
        <Button onClick={() => openModal()}><Plus size={18} /> Add User</Button>
      </div>

      <Card>
        <div className="flex gap-md mb-md items-center">
          <div style={{ position: 'relative', width: '300px' }}>
            <Search size={18} style={{ position: 'absolute', left: '12px', top: '14px', color: '#94A3B8' }} />
            <Input 
              placeholder="Search users..." 
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
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td><span className="badge badge-primary">{user.role}</span></td>
                  <td>
                    <span className={user.status === 'Active' ? 'badge badge-success' : 'badge badge-primary'}>
                      {user.status || 'Active'}
                    </span>
                  </td>
                  <td>
                    <div className="flex gap-sm">
                      <Button variant="ghost" onClick={() => openModal(user)} style={{ padding: '8px' }}><Edit2 size={16} /></Button>
                      <Button variant="ghost" onClick={() => handleDelete(user.id)} style={{ padding: '8px', color: 'var(--color-destructive)' }}><Trash2 size={16} /></Button>
                    </div>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '32px' }}>No users found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingUser?.id ? "Edit User Role" : "Add User"}>
        <form onSubmit={handleSave} className="flex-col gap-md">
          <Input 
            label="Name" 
            value={editingUser?.name || ''} 
            disabled
          />
          <Input 
            label="Email" 
            type="email"
            value={editingUser?.email || ''} 
            disabled
          />
          <div className="flex-col gap-sm">
            <label className="text-sm font-semibold">Role</label>
            <select 
              className="input" 
              value={editingUser?.role || 'User'}
              onChange={e => setEditingUser((prev: any) => ({ ...prev, role: e.target.value }))}
            >
              <option value="ADMIN">Admin</option>
              <option value="USER">User</option>
            </select>
          </div>
          <div className="flex justify-end gap-sm mt-md">
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button type="submit">Save Role</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
