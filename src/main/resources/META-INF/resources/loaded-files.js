// Loaded Files Display - Dynamic fetching and rendering of ingested files

class LoadedFilesController {
    constructor() {
        this.deleteModal = document.getElementById('delete-file-modal');
        this.currentSourceFile = null;

        this.setupEventListeners();
    }

    setupEventListeners() {
        // Delegate click events for delete actions
        document.addEventListener('click', (e) => {
            const button = e.target.closest('button[data-action]');
            if (!button) return;

            const action = button.dataset.action;
            switch (action) {
                case 'delete-file':
                    this.openDeleteModal(button.dataset.sourceFile);
                    break;
                case 'close':
                    this.closeDeleteModal();
                    break;
            }
        });

        // Close on backdrop click
        document.querySelectorAll('#delete-file-modal .modal-backdrop').forEach(backdrop => {
            backdrop.addEventListener('click', () => this.closeDeleteModal());
        });

        // Close on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.closeDeleteModal();
            }
        });

        // Confirm delete button
        const confirmDelete = document.getElementById('confirm-delete-file');
        if (confirmDelete) {
            confirmDelete.addEventListener('click', () => this.confirmDelete());
        }
    }

    openDeleteModal(sourceFile) {
        if (!this.deleteModal || !sourceFile) return;
        this.currentSourceFile = sourceFile;
        document.getElementById('delete-file-name').textContent = sourceFile;
        this.deleteModal.hidden = false;
    }

    closeDeleteModal() {
        if (!this.deleteModal) return;
        this.deleteModal.hidden = true;
        this.currentSourceFile = null;
    }

    confirmDelete() {
        if (!this.currentSourceFile) return;

        const sourceFile = this.currentSourceFile;
        const confirmBtn = document.getElementById('confirm-delete-file');

        // Disable button during request
        confirmBtn.disabled = true;
        confirmBtn.textContent = 'Deleting...';

        fetch(`/api/lore/files?sourceFile=${encodeURIComponent(sourceFile)}`, {
            method: 'DELETE',
            headers: { Accept: 'application/json' }
        })
            .then(async response => {
                if (response.ok) {
                    return response.json().catch(() => null);
                }
                const body = await response.json().catch(() => null);
                const message = body?.error || body?.message || 'Failed to delete file';
                throw new Error(message);
            })
            .then(() => {
                this.closeDeleteModal();
                fetchLoadedFiles();
            })
            .catch(error => {
                console.error('Error deleting file:', error);
                alert(`Failed to delete file. ${error.message}`);
            })
            .finally(() => {
                confirmBtn.disabled = false;
                confirmBtn.textContent = 'Delete';
            });
    }
}

// Fetch and display loaded files
async function fetchLoadedFiles() {
    const contentDiv = document.getElementById('loaded-files-content');
    contentDiv.innerHTML = '<p class="loading-text">Loading...</p>';

    try {
        const response = await fetch('/api/lore/files');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const files = await response.json();

        if (files.length === 0) {
            contentDiv.innerHTML = '<p class="empty-state">No files loaded yet. Upload some documents to get started.</p>';
        } else {
            let html = '<table class="loaded-files-table">';
            html += '<thead><tr><th>Source File</th><th>Embeddings</th><th class="file-actions"></th></tr></thead>';
            html += '<tbody>';

            // Sort files by name
            files.sort((a, b) => a.sourceFile.localeCompare(b.sourceFile));

            let totalEmbeddings = 0;
            for (const file of files) {
                html += '<tr>';
                html += `<td class="file-name">${escapeHtml(file.sourceFile)}</td>`;
                html += `<td class="embedding-count">${file.embeddingCount.toLocaleString()}</td>`;
                html += '<td class="file-actions">';
                html += `<button class="btn btn-sm btn-outline btn-icon" title="Delete File" data-action="delete-file" data-source-file="${escapeHtml(file.sourceFile)}">`;
                html += '<span class="icon-trash">🗑</span>';
                html += '</button>';
                html += '</td>';
                html += '</tr>';
                totalEmbeddings += file.embeddingCount;
            }

            html += '</tbody>';
            html += '<tfoot><tr><th>Total</th><th>' + totalEmbeddings.toLocaleString() + '</th><th class="file-actions"></th></tr></tfoot>';
            html += '</table>';
            contentDiv.innerHTML = html;
        }
    } catch (error) {
        console.error('Error fetching loaded files:', error);
        contentDiv.innerHTML = '<p class="error-text">Failed to load files. ' + escapeHtml(error.message) + '</p>';
    }
}

// Utility function to escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    window.loadedFilesController = new LoadedFilesController();

    // Refresh button handler
    const refreshBtn = document.getElementById('refresh-btn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', fetchLoadedFiles);
    }

    // Initial load
    fetchLoadedFiles();
});
